package com.topsion.rag.repository;

import com.topsion.rag.domain.Entity;
import java.util.List;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface EntityRepository extends ReactiveNeo4jRepository<Entity, Long> {

    Flux<Entity> findByName(String name);

    Flux<Entity> findByType(String type);

    @Query("MATCH (e:Entity) WHERE e.name CONTAINS $name RETURN e")
    Flux<Entity> findByNameContaining(@Param("name") String name);

    @Query("MATCH (e1:Entity)-[r:RELATED_TO]->(e2:Entity) " +
           "WHERE e1.id = $entityId " +
           "RETURN e2, r.relationshipType as relationType, r.strength as strength " +
           "ORDER BY r.strength DESC")
    Flux<Object[]> findRelatedEntities(@Param("entityId") Long entityId);

    @Query("MATCH (e:Entity)-[:CONTAINS_ENTITY]-(c:DocumentChunk)-[:HAS_CHUNK]-(d:Document) " +
           "WHERE e.id = $entityId " +
           "RETURN DISTINCT d")
    Flux<Object> findDocumentsByEntityId(@Param("entityId") Long entityId);

    @Query("MATCH (e1:Entity)-[r1:RELATED_TO]->(e2:Entity)-[r2:RELATED_TO]->(e3:Entity) " +
           "WHERE e1.id = $entityId AND e3.id <> $entityId " +
           "RETURN e3, (r1.strength + r2.strength) / 2 as pathStrength " +
           "ORDER BY pathStrength DESC " +
           "LIMIT $limit")
    Flux<Object[]> findIndirectlyRelatedEntities(@Param("entityId") Long entityId, @Param("limit") int limit);

    @Query("MATCH (e:Entity) " +
           "WHERE e.embedding IS NOT NULL " +
           "RETURN e, " +
           "gds.alpha.similarity.cosine(e.embedding, $queryEmbedding) AS similarity " +
           "ORDER BY similarity DESC " +
           "LIMIT $limit")
    Flux<Object[]> findSimilarEntities(@Param("queryEmbedding") double[] queryEmbedding, @Param("limit") int limit);

    @Query("MATCH (e:Entity) " +
           "RETURN e.type as type, count(e) as count " +
           "ORDER BY count DESC")
    Flux<Object[]> getEntityTypeStatistics();

    Mono<Entity> findByNameAndType(String name, String type);
}