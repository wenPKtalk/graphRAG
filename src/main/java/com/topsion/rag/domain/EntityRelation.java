package com.topsion.rag.domain;

import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class EntityRelation extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Property("relationship_type")
    private String relationshipType; // WORKS_FOR, LOCATED_IN, PART_OF, etc.

    @Property("strength")
    private Double strength; // relationship strength/confidence score

    @Property("description")
    private String description;

    @TargetNode
    private Entity targetEntity;

    public EntityRelation() {}

    public EntityRelation(String relationshipType, Entity targetEntity) {
        this.relationshipType = relationshipType;
        this.targetEntity = targetEntity;
    }

    public Long getId() {
        return this.id;
    }

    public EntityRelation id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRelationshipType() {
        return this.relationshipType;
    }

    public EntityRelation relationshipType(String relationshipType) {
        this.setRelationshipType(relationshipType);
        return this;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public Double getStrength() {
        return this.strength;
    }

    public EntityRelation strength(Double strength) {
        this.setStrength(strength);
        return this;
    }

    public void setStrength(Double strength) {
        this.strength = strength;
    }

    public String getDescription() {
        return this.description;
    }

    public EntityRelation description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Entity getTargetEntity() {
        return this.targetEntity;
    }

    public EntityRelation targetEntity(Entity entity) {
        this.setTargetEntity(entity);
        return this;
    }

    public void setTargetEntity(Entity entity) {
        this.targetEntity = entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityRelation)) {
            return false;
        }
        return getId() != null && getId().equals(((EntityRelation) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "EntityRelation{" +
            "id=" + getId() +
            ", relationshipType='" + getRelationshipType() + "'" +
            ", strength=" + getStrength() +
            ", description='" + getDescription() + "'" +
            "}";
    }
}