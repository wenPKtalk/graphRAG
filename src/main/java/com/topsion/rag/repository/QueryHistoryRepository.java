package com.topsion.rag.repository;

import com.topsion.rag.domain.QueryHistory;
import java.time.Instant;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface QueryHistoryRepository extends ReactiveNeo4jRepository<QueryHistory, Long> {

    Flux<QueryHistory> findBySessionId(String sessionId);

    @Query("MATCH (q:QueryHistory) " +
           "WHERE q.createdDate >= $startDate AND q.createdDate <= $endDate " +
           "RETURN q ORDER BY q.createdDate DESC")
    Flux<QueryHistory> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("MATCH (q:QueryHistory) " +
           "WHERE q.question CONTAINS $keyword " +
           "RETURN q ORDER BY q.createdDate DESC")
    Flux<QueryHistory> findByQuestionContaining(@Param("keyword") String keyword);

    @Query("MATCH (q:QueryHistory) " +
           "WHERE q.userFeedback = $feedback " +
           "RETURN q ORDER BY q.createdDate DESC")
    Flux<QueryHistory> findByUserFeedback(@Param("feedback") String feedback);

    @Query("MATCH (q:QueryHistory) " +
           "RETURN avg(q.responseTimeMs) as avgResponseTime, " +
           "min(q.responseTimeMs) as minResponseTime, " +
           "max(q.responseTimeMs) as maxResponseTime")
    Mono<Object[]> getResponseTimeStatistics();

    @Query("MATCH (q:QueryHistory) " +
           "WHERE q.userFeedback IS NOT NULL " +
           "RETURN q.userFeedback as feedback, count(q) as count")
    Flux<Object[]> getFeedbackStatistics();

    @Query("MATCH (q:QueryHistory) " +
           "WHERE q.sessionId = $sessionId " +
           "RETURN q ORDER BY q.createdDate DESC " +
           "LIMIT $limit")
    Flux<QueryHistory> findRecentQueriesBySession(@Param("sessionId") String sessionId, @Param("limit") int limit);
}