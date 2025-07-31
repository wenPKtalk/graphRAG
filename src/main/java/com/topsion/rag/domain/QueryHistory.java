package com.topsion.rag.domain;

import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("QueryHistory")
public class QueryHistory extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Property("question")
    private String question;

    @Property("answer")
    private String answer;

    @Property("context_chunks")
    private String contextChunks; // JSON array of chunk IDs used for context

    @Property("response_time_ms")
    private Long responseTimeMs;

    @Property("user_feedback")
    private String userFeedback; // HELPFUL, NOT_HELPFUL, etc.

    @Property("session_id")
    private String sessionId;

    public QueryHistory() {}

    public QueryHistory(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public Long getId() {
        return this.id;
    }

    public QueryHistory id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return this.question;
    }

    public QueryHistory question(String question) {
        this.setQuestion(question);
        return this;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return this.answer;
    }

    public QueryHistory answer(String answer) {
        this.setAnswer(answer);
        return this;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getContextChunks() {
        return this.contextChunks;
    }

    public QueryHistory contextChunks(String contextChunks) {
        this.setContextChunks(contextChunks);
        return this;
    }

    public void setContextChunks(String contextChunks) {
        this.contextChunks = contextChunks;
    }

    public Long getResponseTimeMs() {
        return this.responseTimeMs;
    }

    public QueryHistory responseTimeMs(Long responseTimeMs) {
        this.setResponseTimeMs(responseTimeMs);
        return this;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getUserFeedback() {
        return this.userFeedback;
    }

    public QueryHistory userFeedback(String userFeedback) {
        this.setUserFeedback(userFeedback);
        return this;
    }

    public void setUserFeedback(String userFeedback) {
        this.userFeedback = userFeedback;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public QueryHistory sessionId(String sessionId) {
        this.setSessionId(sessionId);
        return this;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryHistory)) {
            return false;
        }
        return getId() != null && getId().equals(((QueryHistory) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "QueryHistory{" +
            "id=" + getId() +
            ", question='" + getQuestion() + "'" +
            ", answer='" + getAnswer() + "'" +
            ", responseTimeMs=" + getResponseTimeMs() +
            ", userFeedback='" + getUserFeedback() + "'" +
            ", sessionId='" + getSessionId() + "'" +
            "}";
    }
}