package com.topsion.rag.web.rest;

import com.topsion.rag.domain.Document;
import com.topsion.rag.service.DocumentProcessingService;
import com.topsion.rag.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api")
public class DocumentResource {

    private final Logger log = LoggerFactory.getLogger(DocumentResource.class);

    private static final String ENTITY_NAME = "document";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DocumentProcessingService documentProcessingService;

    public DocumentResource(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Document>> uploadDocument(@RequestPart("file") Mono<FilePart> filePartMono) {
        log.debug("REST request to upload Document");
        
        return filePartMono
            .flatMap(documentProcessingService::uploadAndProcessDocument)
            .map(document -> {
                try {
                    return ResponseEntity.created(new URI("/api/documents/" + document.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, document.getId().toString()))
                        .body(document);
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Invalid URI syntax", e);
                }
            })
            .onErrorMap(Exception.class, ex -> new BadRequestAlertException("Failed to upload document", ENTITY_NAME, "uploadfailed"));
    }

    @GetMapping("/documents")
    public Flux<Document> getAllDocuments() {
        log.debug("REST request to get all Documents");
        return documentProcessingService.getAllDocuments();
    }

    @GetMapping("/documents/{id}")
    public Mono<ResponseEntity<Document>> getDocument(@PathVariable Long id) {
        log.debug("REST request to get Document : {}", id);
        return documentProcessingService.getDocumentById(id)
            .map(document -> ResponseEntity.ok().body(document))
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/documents/{id}")
    public Mono<ResponseEntity<Void>> deleteDocument(@PathVariable Long id) {
        log.debug("REST request to delete Document : {}", id);
        return documentProcessingService.deleteDocument(id)
            .then(Mono.fromRunnable(() -> {
                log.debug("Document {} deleted successfully", id);
            }))
            .map(aVoid -> ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                .build());
    }
}