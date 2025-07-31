package com.topsion.rag.service;

import com.topsion.rag.domain.DocumentChunk;
import com.topsion.rag.domain.Entity;
import com.topsion.rag.domain.QueryHistory;
import com.topsion.rag.repository.DocumentChunkRepository;
import com.topsion.rag.repository.EntityRepository;
import com.topsion.rag.repository.QueryHistoryRepository;
import com.topsion.rag.config.ApplicationProperties;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RAGQueryService {

    private final Logger log = LoggerFactory.getLogger(RAGQueryService.class);

    private final DocumentChunkRepository documentChunkRepository;
    private final EntityRepository entityRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final OpenAiService openAiService;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String RAG_SYSTEM_PROMPT = """
        你是一个专业的知识问答助手。请基于提供的上下文信息回答用户的问题。

        回答要求：
        1. 仅基于提供的上下文信息回答，不要添加上下文中没有的信息
        2. 如果上下文信息不足以回答问题，请明确说明
        3. 回答要准确、简洁、有条理
        4. 可以引用相关的实体和概念
        5. 用中文回答

        上下文信息：
        %s

        相关实体：
        %s
        """;

    public RAGQueryService(
        DocumentChunkRepository documentChunkRepository,
        EntityRepository entityRepository,
        QueryHistoryRepository queryHistoryRepository,
        OpenAiService openAiService,
        ApplicationProperties applicationProperties
    ) {
        this.documentChunkRepository = documentChunkRepository;
        this.entityRepository = entityRepository;
        this.queryHistoryRepository = queryHistoryRepository;
        this.openAiService = openAiService;
        this.applicationProperties = applicationProperties;
    }

    public Mono<QueryResult> queryKnowledgeBase(String question, String sessionId) {
        long startTime = System.currentTimeMillis();

        return generateQueryEmbedding(question)
            .flatMap(embedding -> retrieveRelevantContext(embedding, question))
            .flatMap(context -> generateAnswer(question, context))
            .flatMap(result -> {
                long responseTime = System.currentTimeMillis() - startTime;
                return saveQueryHistory(question, result.answer(), result.contextChunks(), responseTime, sessionId)
                    .thenReturn(result);
            })
            .onErrorResume(error -> {
                log.error("Error processing query: {}", error.getMessage(), error);
                return Mono.just(new QueryResult(
                    "抱歉，处理您的问题时出现了错误。请稍后重试。",
                    Collections.emptyList(),
                    Collections.emptyList()
                ));
            });
    }

    private Mono<double[]> generateQueryEmbedding(String query) {
        return Mono.fromCallable(() -> {
            try {
                String model = applicationProperties.getOpenai().getModel().getEmbedding();
                EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(model)
                    .input(List.of(query))
                    .build();

                var response = openAiService.createEmbeddings(request);
                if (!response.getData().isEmpty()) {
                    List<Double> embedding = response.getData().get(0).getEmbedding();
                    return embedding.stream().mapToDouble(Double::doubleValue).toArray();
                }

                throw new RuntimeException("Failed to generate embedding");
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate query embedding", e);
            }
        });
    }

    private Mono<RetrievedContext> retrieveRelevantContext(double[] queryEmbedding, String question) {
        int maxChunks = applicationProperties.getOpenai().getRag().getMaxContextChunks();

        return Mono.zip(
            retrieveSimilarChunks(queryEmbedding, maxChunks),
            retrieveRelevantEntities(queryEmbedding, question, 10)
        )
        .map(tuple -> new RetrievedContext(tuple.getT1(), tuple.getT2()));
    }

    private Mono<List<DocumentChunk>> retrieveSimilarChunks(double[] queryEmbedding, int limit) {
        return documentChunkRepository.findSimilarChunks(queryEmbedding, limit)
            .filter(result -> {
                double similarity = (Double) result[1];
                return similarity >= applicationProperties.getOpenai().getRag().getSimilarityThreshold();
            })
            .map(result -> (DocumentChunk) result[0])
            .collectList()
            .doOnNext(chunks -> log.debug("Retrieved {} similar chunks", chunks.size()));
    }

    private Mono<List<Entity>> retrieveRelevantEntities(double[] queryEmbedding, String question, int limit) {
        return Mono.zip(
            // 基于向量相似度检索实体
            entityRepository.findSimilarEntities(queryEmbedding, limit / 2)
                .map(result -> (Entity) result[0])
                .collectList(),
            // 基于关键词检索实体
            extractKeywordsAndSearchEntities(question, limit / 2)
        )
        .map(tuple -> {
            Set<Entity> combinedEntities = new HashSet<>(tuple.getT1());
            combinedEntities.addAll(tuple.getT2());
            return List.copyOf(combinedEntities);
        })
        .doOnNext(entities -> log.debug("Retrieved {} relevant entities", entities.size()));
    }

    private Mono<List<Entity>> extractKeywordsAndSearchEntities(String question, int limit) {
        return Flux.fromArray(question.split("\\s+"))
            .filter(word -> word.length() > 2)
            .flatMap(keyword -> entityRepository.findByNameContaining(keyword))
            .distinct()
            .take(limit)
            .collectList();
    }

    private Mono<QueryResult> generateAnswer(String question, RetrievedContext context) {
        return Mono.fromCallable(() -> {
            try {
                String contextText = buildContextText(context.chunks());
                String entitiesText = buildEntitiesText(context.entities());

                String systemPrompt = String.format(RAG_SYSTEM_PROMPT, contextText, entitiesText);

                ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(applicationProperties.getOpenai().getModel().getChat())
                    .messages(List.of(
                        new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                        new ChatMessage(ChatMessageRole.USER.value(), question)
                    ))
                    .temperature(0.3)
                    .maxTokens(1000)
                    .build();

                var response = openAiService.createChatCompletion(request);
                String answer = response.getChoices().get(0).getMessage().getContent();

                return new QueryResult(answer, context.chunks(), context.entities());

            } catch (Exception e) {
                log.error("Failed to generate answer: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to generate answer", e);
            }
        });
    }

    private String buildContextText(List<DocumentChunk> chunks) {
        return chunks.stream()
            .map(chunk -> String.format("文档片段 %d: %s", chunk.getChunkIndex(), chunk.getContent()))
            .collect(Collectors.joining("\n\n"));
    }

    private String buildEntitiesText(List<Entity> entities) {
        return entities.stream()
            .map(entity -> String.format("- %s (%s): %s",
                entity.getName(),
                entity.getType(),
                entity.getDescription() != null ? entity.getDescription() : ""))
            .collect(Collectors.joining("\n"));
    }

    private Mono<Void> saveQueryHistory(String question, String answer, List<DocumentChunk> contextChunks,
                                       long responseTime, String sessionId) {
        return Mono.fromCallable(() -> {
            try {
                QueryHistory history = new QueryHistory();
                history.setQuestion(question);
                history.setAnswer(answer);
                history.setResponseTimeMs(responseTime);
                history.setSessionId(sessionId);
                history.setCreatedDate(Instant.now());

                List<Long> chunkIds = contextChunks.stream()
                    .map(DocumentChunk::getId)
                    .collect(Collectors.toList());
                history.setContextChunks(objectMapper.writeValueAsString(chunkIds));

                return history;
            } catch (Exception e) {
                log.error("Failed to create query history: {}", e.getMessage(), e);
                return null;
            }
        })
        .filter(Objects::nonNull)
        .flatMap(queryHistoryRepository::save)
        .then();
    }

    public Mono<Void> provideFeedback(Long queryId, String feedback) {
        return queryHistoryRepository.findById(queryId)
            .flatMap(history -> {
                history.setUserFeedback(feedback);
                return queryHistoryRepository.save(history);
            })
            .then();
    }

    public Flux<QueryHistory> getQueryHistory(String sessionId, int limit) {
        return queryHistoryRepository.findRecentQueriesBySession(sessionId, limit);
    }

    public Mono<List<String>> getSuggestions(String partialQuery) {
        return Mono.fromCallable(() -> {
            if (partialQuery.length() < 2) {
                return Collections.<String>emptyList();
            }

            return entityRepository.findByNameContaining(partialQuery)
                .take(5)
                .map(entity -> "关于" + entity.getName() + "的信息")
                .collectList()
                .block();
        });
    }

    public Flux<Entity> exploreRelatedEntities(String entityName) {
        return entityRepository.findByName(entityName)
            .next()
            .flatMapMany(entity ->
                entityRepository.findRelatedEntities(entity.getId())
                    .map(result -> (Entity) result[0])
            );
    }

    public record QueryResult(String answer, List<DocumentChunk> contextChunks, List<Entity> relatedEntities) {}
    public record RetrievedContext(List<DocumentChunk> chunks, List<Entity> entities) {}
}
