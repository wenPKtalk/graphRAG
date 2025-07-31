package com.topsion.rag.web.rest;

import com.topsion.rag.domain.Entity;
import com.topsion.rag.domain.QueryHistory;
import com.topsion.rag.service.RAGQueryService;
import com.topsion.rag.web.rest.vm.QueryRequestVM;
import com.topsion.rag.web.rest.vm.FeedbackVM;
import com.topsion.rag.web.rest.vm.QueryResponseVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class QueryResource {

    private final Logger log = LoggerFactory.getLogger(QueryResource.class);

    private final RAGQueryService ragQueryService;

    public QueryResource(RAGQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    @PostMapping("/query")
    public Mono<ResponseEntity<QueryResponseVM>> query(@Valid @RequestBody QueryRequestVM queryRequest) {
        log.debug("REST request to query knowledge base: {}", queryRequest.question());
        
        String sessionId = queryRequest.sessionId() != null ? queryRequest.sessionId() : UUID.randomUUID().toString();
        
        return ragQueryService.queryKnowledgeBase(queryRequest.question(), sessionId)
            .map(result -> {
                QueryResponseVM response = new QueryResponseVM(
                    result.answer(),
                    result.contextChunks().stream()
                        .map(chunk -> new QueryResponseVM.ContextChunk(
                            chunk.getId(),
                            chunk.getContent(),
                            chunk.getDocument() != null ? chunk.getDocument().getTitle() : null,
                            chunk.getChunkIndex()
                        ))
                        .toList(),
                    result.relatedEntities().stream()
                        .map(entity -> new QueryResponseVM.RelatedEntity(
                            entity.getId(),
                            entity.getName(),
                            entity.getType(),
                            entity.getDescription()
                        ))
                        .toList(),
                    sessionId
                );
                return ResponseEntity.ok(response);
            });
    }

    @PostMapping("/query/{queryId}/feedback")
    public Mono<ResponseEntity<Void>> provideFeedback(@PathVariable Long queryId, @Valid @RequestBody FeedbackVM feedback) {
        log.debug("REST request to provide feedback for query: {}", queryId);
        
        return ragQueryService.provideFeedback(queryId, feedback.feedback())
            .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }

    @GetMapping("/query/history")
    public Flux<QueryHistory> getQueryHistory(@RequestParam String sessionId, @RequestParam(defaultValue = "10") int limit) {
        log.debug("REST request to get query history for session: {}", sessionId);
        return ragQueryService.getQueryHistory(sessionId, limit);
    }

    @GetMapping("/query/suggestions")
    public Mono<ResponseEntity<List<String>>> getSuggestions(@RequestParam String q) {
        log.debug("REST request to get suggestions for: {}", q);
        return ragQueryService.getSuggestions(q)
            .map(suggestions -> ResponseEntity.ok(suggestions));
    }

    @GetMapping("/entities/{entityName}/related")
    public Flux<Entity> getRelatedEntities(@PathVariable String entityName) {
        log.debug("REST request to get related entities for: {}", entityName);
        return ragQueryService.exploreRelatedEntities(entityName);
    }
}