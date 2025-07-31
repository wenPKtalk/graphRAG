package com.topsion.rag.repository;

import com.topsion.rag.domain.Document;
import java.util.List;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DocumentRepository extends ReactiveNeo4jRepository<Document, Long> {

    Flux<Document> findByStatus(String status);

    Flux<Document> findByFilename(String filename);

    @Query("MATCH (d:Document) WHERE d.title CONTAINS $title OR d.filename CONTAINS $title RETURN d")
    Flux<Document> findByTitleContaining(@Param("title") String title);

    @Query("MATCH (d:Document)-[:HAS_CHUNK]->(c:DocumentChunk) WHERE d.id = $documentId RETURN count(c)")
    Mono<Long> countChunksByDocumentId(@Param("documentId") Long documentId);

    @Query("MATCH (d:Document) WHERE d.status = 'PROCESSED' RETURN d ORDER BY d.createdDate DESC")
    Flux<Document> findProcessedDocuments();

    @Query("MATCH (d:Document)-[:HAS_CHUNK]->(c:DocumentChunk)-[:CONTAINS_ENTITY]->(e:Entity) " +
           "WHERE d.id = $documentId RETURN DISTINCT e.name as entityName, e.type as entityType")
    Flux<Object[]> findEntitiesByDocumentId(@Param("documentId") Long documentId);
}