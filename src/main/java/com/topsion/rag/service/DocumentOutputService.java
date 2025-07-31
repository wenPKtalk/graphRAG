package com.topsion.rag.service;

import com.topsion.rag.domain.Document;
import com.topsion.rag.domain.DocumentChunk;
import com.topsion.rag.domain.Entity;
import com.topsion.rag.domain.QueryHistory;
import com.topsion.rag.repository.DocumentRepository;
import com.topsion.rag.repository.QueryHistoryRepository;
import com.topsion.rag.repository.EntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DocumentOutputService {

    private final Logger log = LoggerFactory.getLogger(DocumentOutputService.class);

    private final DocumentRepository documentRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final EntityRepository entityRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentOutputService(
        DocumentRepository documentRepository,
        QueryHistoryRepository queryHistoryRepository,
        EntityRepository entityRepository
    ) {
        this.documentRepository = documentRepository;
        this.queryHistoryRepository = queryHistoryRepository;
        this.entityRepository = entityRepository;
    }

    public Mono<byte[]> generateDocumentSummaryReport(Long documentId, String format) {
        return documentRepository.findById(documentId)
            .flatMap(document -> {
                switch (format.toLowerCase()) {
                    case "json":
                        return generateJsonSummary(document);
                    case "docx":
                        return generateWordSummary(document);
                    case "txt":
                        return generateTextSummary(document);
                    default:
                        return Mono.error(new IllegalArgumentException("Unsupported format: " + format));
                }
            });
    }

    public Mono<byte[]> generateKnowledgeGraphReport(String format) {
        return entityRepository.findAll()
            .collectList()
            .flatMap(entities -> {
                switch (format.toLowerCase()) {
                    case "json":
                        return generateKnowledgeGraphJson(entities);
                    case "docx":
                        return generateKnowledgeGraphWord(entities);
                    case "txt":
                        return generateKnowledgeGraphText(entities);
                    default:
                        return Mono.error(new IllegalArgumentException("Unsupported format: " + format));
                }
            });
    }

    public Mono<byte[]> generateQueryAnalyticsReport(String sessionId, String format) {
        return queryHistoryRepository.findBySessionId(sessionId)
            .collectList()
            .flatMap(queries -> {
                switch (format.toLowerCase()) {
                    case "json":
                        return generateQueryAnalyticsJson(queries);
                    case "docx":
                        return generateQueryAnalyticsWord(queries);
                    case "txt":
                        return generateQueryAnalyticsText(queries);
                    default:
                        return Mono.error(new IllegalArgumentException("Unsupported format: " + format));
                }
            });
    }

    private Mono<byte[]> generateJsonSummary(Document document) {
        return Mono.fromCallable(() -> {
            try {
                ObjectNode root = objectMapper.createObjectNode();
                root.put("id", document.getId());
                root.put("title", document.getTitle());
                root.put("filename", document.getFilename());
                root.put("contentType", document.getContentType());
                root.put("fileSize", document.getFileSize());
                root.put("status", document.getStatus());
                root.put("summary", document.getSummary());
                root.put("createdDate", document.getCreatedDate().toString());

                ArrayNode chunksArray = objectMapper.createArrayNode();
                for (DocumentChunk chunk : document.getChunks()) {
                    ObjectNode chunkNode = objectMapper.createObjectNode();
                    chunkNode.put("id", chunk.getId());
                    chunkNode.put("content", chunk.getContent());
                    chunkNode.put("chunkIndex", chunk.getChunkIndex());
                    chunkNode.put("tokenCount", chunk.getTokenCount());
                    
                    ArrayNode entitiesArray = objectMapper.createArrayNode();
                    for (Entity entity : chunk.getEntities()) {
                        ObjectNode entityNode = objectMapper.createObjectNode();
                        entityNode.put("id", entity.getId());
                        entityNode.put("name", entity.getName());
                        entityNode.put("type", entity.getType());
                        entityNode.put("description", entity.getDescription());
                        entitiesArray.add(entityNode);
                    }
                    chunkNode.set("entities", entitiesArray);
                    chunksArray.add(chunkNode);
                }
                root.set("chunks", chunksArray);

                return objectMapper.writeValueAsBytes(root);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate JSON summary", e);
            }
        });
    }

    private Mono<byte[]> generateWordSummary(Document document) {
        return Mono.fromCallable(() -> {
            try (XWPFDocument doc = new XWPFDocument();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                // Title
                XWPFParagraph titlePara = doc.createParagraph();
                XWPFRun titleRun = titlePara.createRun();
                titleRun.setText("文档分析报告: " + document.getTitle());
                titleRun.setBold(true);
                titleRun.setFontSize(16);

                // Document Info
                doc.createParagraph().createRun().setText("");
                XWPFParagraph infoPara = doc.createParagraph();
                XWPFRun infoRun = infoPara.createRun();
                infoRun.setText("文档信息:");
                infoRun.setBold(true);

                doc.createParagraph().createRun().setText("文件名: " + document.getFilename());
                doc.createParagraph().createRun().setText("文件大小: " + formatFileSize(document.getFileSize()));
                doc.createParagraph().createRun().setText("状态: " + document.getStatus());
                doc.createParagraph().createRun().setText("上传时间: " + 
                    document.getCreatedDate().atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                // Summary
                doc.createParagraph().createRun().setText("");
                XWPFParagraph summaryPara = doc.createParagraph();
                XWPFRun summaryRun = summaryPara.createRun();
                summaryRun.setText("文档摘要:");
                summaryRun.setBold(true);

                doc.createParagraph().createRun().setText(document.getSummary());

                // Chunks Analysis
                doc.createParagraph().createRun().setText("");
                XWPFParagraph chunksPara = doc.createParagraph();
                XWPFRun chunksRun = chunksPara.createRun();
                chunksRun.setText("内容分析:");
                chunksRun.setBold(true);

                doc.createParagraph().createRun().setText("总分片数: " + document.getChunks().size());

                // Entities Summary
                Set<Entity> allEntities = document.getChunks().stream()
                    .flatMap(chunk -> chunk.getEntities().stream())
                    .collect(Collectors.toSet());

                Map<String, Long> entityTypeCount = allEntities.stream()
                    .collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));

                doc.createParagraph().createRun().setText("实体统计:");
                for (Map.Entry<String, Long> entry : entityTypeCount.entrySet()) {
                    doc.createParagraph().createRun().setText("  " + entry.getKey() + ": " + entry.getValue());
                }

                // Top Entities
                doc.createParagraph().createRun().setText("");
                XWPFParagraph entitiesPara = doc.createParagraph();
                XWPFRun entitiesRun = entitiesPara.createRun();
                entitiesRun.setText("主要实体:");
                entitiesRun.setBold(true);

                allEntities.stream()
                    .limit(20)
                    .forEach(entity -> {
                        doc.createParagraph().createRun().setText("• " + entity.getName() + " (" + entity.getType() + ")");
                        if (entity.getDescription() != null && !entity.getDescription().isEmpty()) {
                            doc.createParagraph().createRun().setText("  " + entity.getDescription());
                        }
                    });

                doc.write(out);
                return out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate Word summary", e);
            }
        });
    }

    private Mono<byte[]> generateTextSummary(Document document) {
        return Mono.fromCallable(() -> {
            StringBuilder sb = new StringBuilder();
            
            sb.append("文档分析报告: ").append(document.getTitle()).append("\n");
            sb.append("=".repeat(50)).append("\n\n");
            
            sb.append("文档信息:\n");
            sb.append("文件名: ").append(document.getFilename()).append("\n");
            sb.append("文件大小: ").append(formatFileSize(document.getFileSize())).append("\n");
            sb.append("状态: ").append(document.getStatus()).append("\n");
            sb.append("上传时间: ").append(
                document.getCreatedDate().atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ).append("\n\n");
            
            sb.append("文档摘要:\n");
            sb.append(document.getSummary()).append("\n\n");
            
            sb.append("内容分析:\n");
            sb.append("总分片数: ").append(document.getChunks().size()).append("\n\n");
            
            Set<Entity> allEntities = document.getChunks().stream()
                .flatMap(chunk -> chunk.getEntities().stream())
                .collect(Collectors.toSet());
                
            Map<String, Long> entityTypeCount = allEntities.stream()
                .collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));
                
            sb.append("实体统计:\n");
            for (Map.Entry<String, Long> entry : entityTypeCount.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
            
            sb.append("主要实体:\n");
            allEntities.stream()
                .limit(20)
                .forEach(entity -> {
                    sb.append("• ").append(entity.getName()).append(" (").append(entity.getType()).append(")\n");
                    if (entity.getDescription() != null && !entity.getDescription().isEmpty()) {
                        sb.append("  ").append(entity.getDescription()).append("\n");
                    }
                });
            
            return sb.toString().getBytes();
        });
    }

    private Mono<byte[]> generateKnowledgeGraphJson(List<Entity> entities) {
        return Mono.fromCallable(() -> {
            try {
                ObjectNode root = objectMapper.createObjectNode();
                root.put("totalEntities", entities.size());
                
                Map<String, Long> typeCount = entities.stream()
                    .collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));
                
                ObjectNode typeStats = objectMapper.createObjectNode();
                typeCount.forEach(typeStats::put);
                root.set("entityTypeStatistics", typeStats);
                
                ArrayNode entitiesArray = objectMapper.createArrayNode();
                for (Entity entity : entities) {
                    ObjectNode entityNode = objectMapper.createObjectNode();
                    entityNode.put("id", entity.getId());
                    entityNode.put("name", entity.getName());
                    entityNode.put("type", entity.getType());
                    entityNode.put("description", entity.getDescription());
                    entityNode.put("documentCount", entity.getChunks().size());
                    entitiesArray.add(entityNode);
                }
                root.set("entities", entitiesArray);
                
                return objectMapper.writeValueAsBytes(root);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate knowledge graph JSON", e);
            }
        });
    }

    private Mono<byte[]> generateKnowledgeGraphWord(List<Entity> entities) {
        return Mono.fromCallable(() -> {
            try (XWPFDocument doc = new XWPFDocument();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                // Title
                XWPFParagraph titlePara = doc.createParagraph();
                XWPFRun titleRun = titlePara.createRun();
                titleRun.setText("知识图谱分析报告");
                titleRun.setBold(true);
                titleRun.setFontSize(16);

                // Statistics
                doc.createParagraph().createRun().setText("");
                doc.createParagraph().createRun().setText("总实体数: " + entities.size());

                Map<String, Long> typeCount = entities.stream()
                    .collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));

                doc.createParagraph().createRun().setText("");
                XWPFParagraph typesPara = doc.createParagraph();
                XWPFRun typesRun = typesPara.createRun();
                typesRun.setText("实体类型分布:");
                typesRun.setBold(true);

                for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
                    doc.createParagraph().createRun().setText("  " + entry.getKey() + ": " + entry.getValue());
                }

                // Entity Details
                doc.createParagraph().createRun().setText("");
                XWPFParagraph detailsPara = doc.createParagraph();
                XWPFRun detailsRun = detailsPara.createRun();
                detailsRun.setText("实体详情:");
                detailsRun.setBold(true);

                entities.stream()
                    .limit(100) // Limit to prevent overly large documents
                    .forEach(entity -> {
                        doc.createParagraph().createRun().setText("• " + entity.getName() + " (" + entity.getType() + ")");
                        if (entity.getDescription() != null && !entity.getDescription().isEmpty()) {
                            doc.createParagraph().createRun().setText("  描述: " + entity.getDescription());
                        }
                        doc.createParagraph().createRun().setText("  相关文档片段数: " + entity.getChunks().size());
                    });

                doc.write(out);
                return out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate knowledge graph Word report", e);
            }
        });
    }

    private Mono<byte[]> generateKnowledgeGraphText(List<Entity> entities) {
        return Mono.fromCallable(() -> {
            StringBuilder sb = new StringBuilder();
            
            sb.append("知识图谱分析报告\n");
            sb.append("=".repeat(30)).append("\n\n");
            
            sb.append("总实体数: ").append(entities.size()).append("\n\n");
            
            Map<String, Long> typeCount = entities.stream()
                .collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));
                
            sb.append("实体类型分布:\n");
            for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
            
            sb.append("实体详情:\n");
            entities.stream()
                .limit(100)
                .forEach(entity -> {
                    sb.append("• ").append(entity.getName()).append(" (").append(entity.getType()).append(")\n");
                    if (entity.getDescription() != null && !entity.getDescription().isEmpty()) {
                        sb.append("  描述: ").append(entity.getDescription()).append("\n");
                    }
                    sb.append("  相关文档片段数: ").append(entity.getChunks().size()).append("\n");
                });
            
            return sb.toString().getBytes();
        });
    }

    private Mono<byte[]> generateQueryAnalyticsJson(List<QueryHistory> queries) {
        return Mono.fromCallable(() -> {
            try {
                ObjectNode root = objectMapper.createObjectNode();
                root.put("totalQueries", queries.size());
                
                if (!queries.isEmpty()) {
                    double avgResponseTime = queries.stream()
                        .filter(q -> q.getResponseTimeMs() != null)
                        .mapToLong(QueryHistory::getResponseTimeMs)
                        .average()
                        .orElse(0.0);
                    
                    root.put("averageResponseTimeMs", avgResponseTime);
                }
                
                ArrayNode queriesArray = objectMapper.createArrayNode();
                for (QueryHistory query : queries) {
                    ObjectNode queryNode = objectMapper.createObjectNode();
                    queryNode.put("id", query.getId());
                    queryNode.put("question", query.getQuestion());
                    queryNode.put("answer", query.getAnswer());
                    queryNode.put("responseTimeMs", query.getResponseTimeMs());
                    queryNode.put("userFeedback", query.getUserFeedback());
                    queryNode.put("createdDate", query.getCreatedDate().toString());
                    queriesArray.add(queryNode);
                }
                root.set("queries", queriesArray);
                
                return objectMapper.writeValueAsBytes(root);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate query analytics JSON", e);
            }
        });
    }

    private Mono<byte[]> generateQueryAnalyticsWord(List<QueryHistory> queries) {
        return Mono.fromCallable(() -> {
            try (XWPFDocument doc = new XWPFDocument();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                // Title
                XWPFParagraph titlePara = doc.createParagraph();
                XWPFRun titleRun = titlePara.createRun();
                titleRun.setText("查询分析报告");
                titleRun.setBold(true);
                titleRun.setFontSize(16);

                // Statistics
                doc.createParagraph().createRun().setText("");
                doc.createParagraph().createRun().setText("总查询数: " + queries.size());

                if (!queries.isEmpty()) {
                    double avgResponseTime = queries.stream()
                        .filter(q -> q.getResponseTimeMs() != null)
                        .mapToLong(QueryHistory::getResponseTimeMs)
                        .average()
                        .orElse(0.0);
                    
                    doc.createParagraph().createRun().setText("平均响应时间: " + String.format("%.1f", avgResponseTime) + "ms");
                }

                // Query Details
                doc.createParagraph().createRun().setText("");
                XWPFParagraph detailsPara = doc.createParagraph();
                XWPFRun detailsRun = detailsPara.createRun();
                detailsRun.setText("查询详情:");
                detailsRun.setBold(true);

                queries.stream()
                    .limit(50) // Limit to prevent overly large documents
                    .forEach(query -> {
                        doc.createParagraph().createRun().setText("");
                        XWPFParagraph questionPara = doc.createParagraph();
                        XWPFRun questionRun = questionPara.createRun();
                        questionRun.setText("问题: " + query.getQuestion());
                        questionRun.setBold(true);
                        
                        doc.createParagraph().createRun().setText("答案: " + 
                            (query.getAnswer().length() > 200 ? query.getAnswer().substring(0, 200) + "..." : query.getAnswer()));
                        
                        if (query.getResponseTimeMs() != null) {
                            doc.createParagraph().createRun().setText("响应时间: " + query.getResponseTimeMs() + "ms");
                        }
                        
                        doc.createParagraph().createRun().setText("时间: " + 
                            query.getCreatedDate().atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    });

                doc.write(out);
                return out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate query analytics Word report", e);
            }
        });
    }

    private Mono<byte[]> generateQueryAnalyticsText(List<QueryHistory> queries) {
        return Mono.fromCallable(() -> {
            StringBuilder sb = new StringBuilder();
            
            sb.append("查询分析报告\n");
            sb.append("=".repeat(20)).append("\n\n");
            
            sb.append("总查询数: ").append(queries.size()).append("\n");
            
            if (!queries.isEmpty()) {
                double avgResponseTime = queries.stream()
                    .filter(q -> q.getResponseTimeMs() != null)
                    .mapToLong(QueryHistory::getResponseTimeMs)
                    .average()
                    .orElse(0.0);
                
                sb.append("平均响应时间: ").append(String.format("%.1f", avgResponseTime)).append("ms\n");
            }
            sb.append("\n");
            
            sb.append("查询详情:\n");
            queries.stream()
                .limit(50)
                .forEach(query -> {
                    sb.append("\n问题: ").append(query.getQuestion()).append("\n");
                    sb.append("答案: ").append(
                        query.getAnswer().length() > 200 ? query.getAnswer().substring(0, 200) + "..." : query.getAnswer()
                    ).append("\n");
                    
                    if (query.getResponseTimeMs() != null) {
                        sb.append("响应时间: ").append(query.getResponseTimeMs()).append("ms\n");
                    }
                    
                    sb.append("时间: ").append(
                        query.getCreatedDate().atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    ).append("\n");
                    sb.append("-".repeat(50)).append("\n");
                });
            
            return sb.toString().getBytes();
        });
    }

    private String formatFileSize(long bytes) {
        if (bytes == 0) return "0 Bytes";
        int k = 1024;
        String[] sizes = {"Bytes", "KB", "MB", "GB"};
        int i = (int) Math.floor(Math.log(bytes) / Math.log(k));
        return String.format("%.1f %s", bytes / Math.pow(k, i), sizes[i]);
    }
}