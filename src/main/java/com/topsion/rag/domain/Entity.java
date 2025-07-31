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

@Node("Entity")
public class Entity extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("type")
    private String type; // PERSON, ORGANIZATION, LOCATION, CONCEPT, etc.

    @Property("description")
    private String description;

    @Property("embedding")
    private double[] embedding;

    @Relationship(type = "CONTAINS_ENTITY", direction = Relationship.Direction.INCOMING)
    @JsonIgnoreProperties(value = { "entities" }, allowSetters = true)
    private Set<DocumentChunk> chunks = new HashSet<>();

    @Relationship(type = "RELATED_TO")
    @JsonIgnoreProperties(value = { "relatedEntities" }, allowSetters = true)
    private Set<EntityRelation> relations = new HashSet<>();

    public Entity() {}

    public Entity(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Long getId() {
        return this.id;
    }

    public Entity id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Entity name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public Entity type(String type) {
        this.setType(type);
        return this;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return this.description;
    }

    public Entity description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double[] getEmbedding() {
        return this.embedding;
    }

    public Entity embedding(double[] embedding) {
        this.setEmbedding(embedding);
        return this;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }

    public Set<DocumentChunk> getChunks() {
        return this.chunks;
    }

    public void setChunks(Set<DocumentChunk> documentChunks) {
        if (this.chunks != null) {
            this.chunks.forEach(i -> i.removeEntity(this));
        }
        if (documentChunks != null) {
            documentChunks.forEach(i -> i.addEntity(this));
        }
        this.chunks = documentChunks;
    }

    public Entity chunks(Set<DocumentChunk> documentChunks) {
        this.setChunks(documentChunks);
        return this;
    }

    public Entity addChunk(DocumentChunk documentChunk) {
        this.chunks.add(documentChunk);
        documentChunk.getEntities().add(this);
        return this;
    }

    public Entity removeChunk(DocumentChunk documentChunk) {
        this.chunks.remove(documentChunk);
        documentChunk.getEntities().remove(this);
        return this;
    }

    public Set<EntityRelation> getRelations() {
        return this.relations;
    }

    public void setRelations(Set<EntityRelation> entityRelations) {
        this.relations = entityRelations;
    }

    public Entity relations(Set<EntityRelation> entityRelations) {
        this.setRelations(entityRelations);
        return this;
    }

    public Entity addRelation(EntityRelation entityRelation) {
        this.relations.add(entityRelation);
        return this;
    }

    public Entity removeRelation(EntityRelation entityRelation) {
        this.relations.remove(entityRelation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Entity)) {
            return false;
        }
        return getId() != null && getId().equals(((Entity) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Entity{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", type='" + getType() + "'" +
            ", description='" + getDescription() + "'" +
            "}";
    }
}