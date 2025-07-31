package com.topsion.rag.web.rest;

import com.topsion.rag.service.DocumentOutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ReportResource {

    private final Logger log = LoggerFactory.getLogger(ReportResource.class);

    private final DocumentOutputService documentOutputService;

    public ReportResource(DocumentOutputService documentOutputService) {
        this.documentOutputService = documentOutputService;
    }

    @GetMapping("/reports/documents/{documentId}")
    public Mono<ResponseEntity<byte[]>> generateDocumentReport(
        @PathVariable Long documentId,
        @RequestParam(defaultValue = "json") String format) {
        
        log.debug("REST request to generate document report for document: {}, format: {}", documentId, format);
        
        return documentOutputService.generateDocumentSummaryReport(documentId, format)
            .map(data -> {
                String filename = "document_report_" + documentId + "." + format;
                MediaType mediaType = getMediaType(format);
                
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(mediaType)
                    .body(data);
            });
    }

    @GetMapping("/reports/knowledge-graph")
    public Mono<ResponseEntity<byte[]>> generateKnowledgeGraphReport(
        @RequestParam(defaultValue = "json") String format) {
        
        log.debug("REST request to generate knowledge graph report, format: {}", format);
        
        return documentOutputService.generateKnowledgeGraphReport(format)
            .map(data -> {
                String filename = "knowledge_graph_report." + format;
                MediaType mediaType = getMediaType(format);
                
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(mediaType)
                    .body(data);
            });
    }

    @GetMapping("/reports/query-analytics")
    public Mono<ResponseEntity<byte[]>> generateQueryAnalyticsReport(
        @RequestParam String sessionId,
        @RequestParam(defaultValue = "json") String format) {
        
        log.debug("REST request to generate query analytics report for session: {}, format: {}", sessionId, format);
        
        return documentOutputService.generateQueryAnalyticsReport(sessionId, format)
            .map(data -> {
                String filename = "query_analytics_report." + format;
                MediaType mediaType = getMediaType(format);
                
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(mediaType)
                    .body(data);
            });
    }

    private MediaType getMediaType(String format) {
        switch (format.toLowerCase()) {
            case "json":
                return MediaType.APPLICATION_JSON;
            case "docx":
                return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "txt":
                return MediaType.TEXT_PLAIN;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}