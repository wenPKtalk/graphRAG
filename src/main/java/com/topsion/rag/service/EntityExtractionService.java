package com.topsion.rag.service;

import com.topsion.rag.domain.Document;
import com.topsion.rag.domain.DocumentChunk;
import com.topsion.rag.domain.Entity;
import com.topsion.rag.domain.EntityRelation;
import com.topsion.rag.repository.EntityRepository;
import com.topsion.rag.repository.DocumentChunkRepository;
import com.topsion.rag.config.ApplicationProperties;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class EntityExtractionService {

    private final Logger log = LoggerFactory.getLogger(EntityExtractionService.class);

    private final EntityRepository entityRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final OpenAiService openAiService;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ENTITY_EXTRACTION_PROMPT = """
        请从以下文本中提取实体信息，并以JSON格式返回。需要提取的实体类型包括：
        - PERSON: 人名
        - ORGANIZATION: 组织、公司、机构名
        - LOCATION: 地点、地名
        - CONCEPT: 重要概念、术语
        - PRODUCT: 产品名称
        - EVENT: 事件名称
        
        请按以下JSON格式返回：
        {
          "entities": [
            {
              "name": "实体名称",
              "type": "实体类型",
              "description": "简短描述"
            }
          ],
          "relations": [
            {
              "source": "源实体名称",
              "target": "目标实体名称",
              "relationship": "关系类型",
              "description": "关系描述"
            }
          ]
        }
        
        文本内容：
        %s
        """;

    public EntityExtractionService(
        EntityRepository entityRepository,
        DocumentChunkRepository documentChunkRepository,
        OpenAiService openAiService,
        ApplicationProperties applicationProperties
    ) {
        this.entityRepository = entityRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.openAiService = openAiService;
        this.applicationProperties = applicationProperties;
    }

    public Mono<Void> extractEntitiesFromDocument(Document document) {
        return Flux.fromIterable(document.getChunks())
            .flatMap(this::extractEntitiesFromChunk)
            .then();
    }

    public Mono<Void> extractEntitiesFromChunk(DocumentChunk chunk) {
        return extractEntitiesUsingLLM(chunk.getContent())
            .flatMap(extractionResult -> 
                processExtractionResult(chunk, extractionResult)
            )
            .onErrorResume(error -> {
                log.error("Failed to extract entities from chunk {}: {}", chunk.getId(), error.getMessage());
                return Mono.empty();
            });
    }

    private Mono<ExtractionResult> extractEntitiesUsingLLM(String content) {
        return Mono.fromCallable(() -> {
            try {
                String prompt = String.format(ENTITY_EXTRACTION_PROMPT, content);
                
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(applicationProperties.getOpenai().getModel().getChat())
                    .messages(List.of(
                        new ChatMessage(ChatMessageRole.USER.value(), prompt)
                    ))
                    .temperature(0.1)
                    .maxTokens(1000)
                    .build();

                var response = openAiService.createChatCompletion(request);
                String jsonResponse = response.getChoices().get(0).getMessage().getContent();
                
                return parseExtractionResult(jsonResponse);
            } catch (Exception e) {
                log.error("Failed to extract entities using LLM: {}", e.getMessage(), e);
                return new ExtractionResult(Collections.emptyList(), Collections.emptyList());
            }
        });
    }

    private ExtractionResult parseExtractionResult(String jsonResponse) {
        try {
            String cleanJson = extractJsonFromResponse(jsonResponse);
            JsonNode rootNode = objectMapper.readTree(cleanJson);
            
            List<EntityInfo> entities = new ArrayList<>();
            List<RelationInfo> relations = new ArrayList<>();
            
            if (rootNode.has("entities")) {
                JsonNode entitiesNode = rootNode.get("entities");
                for (JsonNode entityNode : entitiesNode) {
                    String name = entityNode.get("name").asText();
                    String type = entityNode.get("type").asText();
                    String description = entityNode.has("description") 
                        ? entityNode.get("description").asText() 
                        : "";
                    
                    entities.add(new EntityInfo(name, type, description));
                }
            }
            
            if (rootNode.has("relations")) {
                JsonNode relationsNode = rootNode.get("relations");
                for (JsonNode relationNode : relationsNode) {
                    String source = relationNode.get("source").asText();
                    String target = relationNode.get("target").asText();
                    String relationship = relationNode.get("relationship").asText();
                    String description = relationNode.has("description") 
                        ? relationNode.get("description").asText() 
                        : "";
                    
                    relations.add(new RelationInfo(source, target, relationship, description));
                }
            }
            
            return new ExtractionResult(entities, relations);
        } catch (Exception e) {
            log.error("Failed to parse extraction result: {}", e.getMessage(), e);
            return new ExtractionResult(Collections.emptyList(), Collections.emptyList());
        }
    }

    private String extractJsonFromResponse(String response) {
        Pattern jsonPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = jsonPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        return response;
    }

    private Mono<Void> processExtractionResult(DocumentChunk chunk, ExtractionResult result) {
        return Flux.fromIterable(result.entities())
            .flatMap(entityInfo -> createOrUpdateEntity(entityInfo, chunk))
            .collectList()
            .flatMap(entities -> {
                chunk.setEntities(new HashSet<>(entities));
                return documentChunkRepository.save(chunk);
            })
            .then(processRelations(result.relations()));
    }

    private Mono<Entity> createOrUpdateEntity(EntityInfo entityInfo, DocumentChunk chunk) {
        return entityRepository.findByNameAndType(entityInfo.name(), entityInfo.type())
            .switchIfEmpty(Mono.defer(() -> createNewEntity(entityInfo)))
            .flatMap(entity -> {
                entity.addChunk(chunk);
                if (entity.getDescription() == null || entity.getDescription().isEmpty()) {
                    entity.setDescription(entityInfo.description());
                }
                return generateEntityEmbedding(entity);
            })
            .flatMap(entityRepository::save);
    }

    private Mono<Entity> createNewEntity(EntityInfo entityInfo) {
        return Mono.fromCallable(() -> {
            Entity entity = new Entity();
            entity.setName(entityInfo.name());
            entity.setType(entityInfo.type());
            entity.setDescription(entityInfo.description());
            entity.setCreatedDate(Instant.now());
            return entity;
        });
    }

    private Mono<Entity> generateEntityEmbedding(Entity entity) {
        return Mono.fromCallable(() -> {
            try {
                String textToEmbed = entity.getName() + " " + 
                    (entity.getDescription() != null ? entity.getDescription() : "");
                
                String model = applicationProperties.getOpenai().getModel().getEmbedding();
                EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(model)
                    .input(List.of(textToEmbed))
                    .build();
                
                var response = openAiService.createEmbeddings(request);
                if (!response.getData().isEmpty()) {
                    List<Double> embedding = response.getData().get(0).getEmbedding();
                    entity.setEmbedding(embedding.stream().mapToDouble(Double::doubleValue).toArray());
                }
                
                return entity;
            } catch (Exception e) {
                log.error("Failed to generate embedding for entity: {}", e.getMessage(), e);
                return entity;
            }
        });
    }

    private Mono<Void> processRelations(List<RelationInfo> relations) {
        return Flux.fromIterable(relations)
            .flatMap(this::createEntityRelation)
            .then();
    }

    private Mono<Void> createEntityRelation(RelationInfo relationInfo) {
        return Mono.zip(
            findEntityByName(relationInfo.source()),
            findEntityByName(relationInfo.target())
        )
        .flatMap(tuple -> {
            Entity sourceEntity = tuple.getT1();
            Entity targetEntity = tuple.getT2();
            
            if (sourceEntity != null && targetEntity != null) {
                EntityRelation relation = new EntityRelation();
                relation.setRelationshipType(relationInfo.relationship());
                relation.setDescription(relationInfo.description());
                relation.setStrength(1.0);
                relation.setTargetEntity(targetEntity);
                relation.setCreatedDate(Instant.now());
                
                sourceEntity.addRelation(relation);
                return entityRepository.save(sourceEntity).then();
            }
            return Mono.empty();
        })
        .onErrorResume(error -> {
            log.error("Failed to create relation: {}", error.getMessage());
            return Mono.empty();
        });
    }

    private Mono<Entity> findEntityByName(String name) {
        return entityRepository.findByName(name)
            .next()
            .onErrorReturn(null);
    }

    public Flux<Entity> searchSimilarEntities(String query, int limit) {
        return generateQueryEmbedding(query)
            .flatMapMany(embedding -> 
                entityRepository.findSimilarEntities(embedding, limit)
            )
            .map(result -> (Entity) result[0])
            .onErrorResume(error -> {
                log.error("Failed to search similar entities: {}", error.getMessage());
                return Flux.empty();
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

    public record EntityInfo(String name, String type, String description) {}
    public record RelationInfo(String source, String target, String relationship, String description) {}
    public record ExtractionResult(List<EntityInfo> entities, List<RelationInfo> relations) {}
}