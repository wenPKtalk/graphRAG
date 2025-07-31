package com.topsion.rag.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("DocumentChunk")
public class DocumentChunk extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Property("content")
    private String content;

    @Property("chunk_index")
    private Integer chunkIndex;

    @Property("embedding")
    private double[] embedding;

    @Property("token_count")
    private Integer tokenCount;

    @Relationship(type = "HAS_CHUNK", direction = Relationship.Direction.INCOMING)
    @JsonIgnoreProperties(value = { "chunks" }, allowSetters = true)
    private Document document;

    @Relationship(type = "CONTAINS_ENTITY")
    @JsonIgnoreProperties(value = { "chunks" }, allowSetters = true)
    private Set<Entity> entities = new HashSet<>();

    public DocumentChunk() {}

    public DocumentChunk(String content, Integer chunkIndex) {
        this.content = content;
        this.chunkIndex = chunkIndex;
    }

    public Long getId() {
        return this.id;
    }

    public DocumentChunk id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return this.content;
    }

    public DocumentChunk content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getChunkIndex() {
        return this.chunkIndex;
    }

    public DocumentChunk chunkIndex(Integer chunkIndex) {
        this.setChunkIndex(chunkIndex);
        return this;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public double[] getEmbedding() {
        return this.embedding;
    }

    public DocumentChunk embedding(double[] embedding) {
        this.setEmbedding(embedding);
        return this;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }

    public Integer getTokenCount() {
        return this.tokenCount;
    }

    public DocumentChunk tokenCount(Integer tokenCount) {
        this.setTokenCount(tokenCount);
        return this;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public Document getDocument() {
        return this.document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public DocumentChunk document(Document document) {
        this.setDocument(document);
        return this;
    }

    public Set<Entity> getEntities() {
        return this.entities;
    }

    public void setEntities(Set<Entity> entities) {
        if (this.entities != null) {
            this.entities.forEach(i -> i.removeChunk(this));
        }
        if (entities != null) {
            entities.forEach(i -> i.addChunk(this));
        }
        this.entities = entities;
    }

    public DocumentChunk entities(Set<Entity> entities) {
        this.setEntities(entities);
        return this;
    }

    public DocumentChunk addEntity(Entity entity) {
        this.entities.add(entity);
        entity.getChunks().add(this);
        return this;
    }

    public DocumentChunk removeEntity(Entity entity) {
        this.entities.remove(entity);
        entity.getChunks().remove(this);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentChunk)) {
            return false;
        }
        return getId() != null && getId().equals(((DocumentChunk) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DocumentChunk{" +
            "id=" + getId() +
            ", content='" + getContent() + "'" +
            ", chunkIndex=" + getChunkIndex() +
            ", tokenCount=" + getTokenCount() +
            "}";
    }
}