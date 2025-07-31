package com.topsion.rag.web.rest.vm;

import java.util.List;

public record QueryResponseVM(
    String answer,
    List<ContextChunk> contextChunks,
    List<RelatedEntity> relatedEntities,
    String sessionId
) {
    public record ContextChunk(
        Long id,
        String content,
        String documentTitle,
        Integer chunkIndex
    ) {}
    
    public record RelatedEntity(
        Long id,
        String name,
        String type,
        String description
    ) {}
}