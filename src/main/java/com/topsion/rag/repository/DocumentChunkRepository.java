package com.topsion.rag.repository;

import com.topsion.rag.domain.DocumentChunk;
import java.util.List;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DocumentChunkRepository extends ReactiveNeo4jRepository<DocumentChunk, Long> {

    Flux<DocumentChunk> findByDocumentId(Long documentId);

    @Query("MATCH (c:DocumentChunk) WHERE c.content CONTAINS $keyword RETURN c")
    Flux<DocumentChunk> findByContentContaining(@Param("keyword") String keyword);

    @Query("MATCH (c:DocumentChunk)-[:CONTAINS_ENTITY]->(e:Entity) " +
           "WHERE e.name = $entityName RETURN c")
    Flux<DocumentChunk> findByEntityName(@Param("entityName") String entityName);

    @Query("MATCH (c:DocumentChunk) " +
           "WHERE c.embedding IS NOT NULL " +
           "RETURN c, " +
           "gds.alpha.similarity.cosine(c.embedding, $queryEmbedding) AS similarity " +
           "ORDER BY similarity DESC " +
           "LIMIT $limit")
    Flux<Object[]> findSimilarChunks(@Param("queryEmbedding") double[] queryEmbedding, @Param("limit") int limit);

    @Query("MATCH (c:DocumentChunk)-[r:CONTAINS_ENTITY]->(e:Entity) " +
           "WHERE c.id = $chunkId " +
           "RETURN e")
    Flux<Object> findEntitiesByChunkId(@Param("chunkId") Long chunkId);

    @Query("MATCH (c1:DocumentChunk)-[:CONTAINS_ENTITY]->(e:Entity)<-[:CONTAINS_ENTITY]-(c2:DocumentChunk) " +
           "WHERE c1.id = $chunkId AND c1.id <> c2.id " +
           "RETURN c2, count(e) as sharedEntities " +
           "ORDER BY sharedEntities DESC " +
           "LIMIT $limit")
    Flux<Object[]> findRelatedChunksBySharedEntities(@Param("chunkId") Long chunkId, @Param("limit") int limit);
}