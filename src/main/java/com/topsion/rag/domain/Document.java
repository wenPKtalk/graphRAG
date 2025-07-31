package com.topsion.rag.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Document")
public class Document extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Property("title")
    private String title;

    @Property("filename")
    private String filename;

    @Property("content_type")
    private String contentType;

    @Property("file_size")
    private Long fileSize;

    @Property("file_path")
    private String filePath;

    @Property("status")
    private String status; // UPLOADED, PROCESSING, PROCESSED, ERROR

    @Property("summary")
    private String summary;

    @Relationship(type = "HAS_CHUNK")
    @JsonIgnoreProperties(value = { "document" }, allowSetters = true)
    private Set<DocumentChunk> chunks = new HashSet<>();

    public Document() {}

    public Document(String title, String filename, String contentType) {
        this.title = title;
        this.filename = filename;
        this.contentType = contentType;
    }

    public Long getId() {
        return this.id;
    }

    public Document id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Document title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilename() {
        return this.filename;
    }

    public Document filename(String filename) {
        this.setFilename(filename);
        return this;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return this.contentType;
    }

    public Document contentType(String contentType) {
        this.setContentType(contentType);
        return this;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return this.fileSize;
    }

    public Document fileSize(Long fileSize) {
        this.setFileSize(fileSize);
        return this;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public Document filePath(String filePath) {
        this.setFilePath(filePath);
        return this;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatus() {
        return this.status;
    }

    public Document status(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return this.summary;
    }

    public Document summary(String summary) {
        this.setSummary(summary);
        return this;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Set<DocumentChunk> getChunks() {
        return this.chunks;
    }

    public void setChunks(Set<DocumentChunk> documentChunks) {
        if (this.chunks != null) {
            this.chunks.forEach(i -> i.setDocument(null));
        }
        if (documentChunks != null) {
            documentChunks.forEach(i -> i.setDocument(this));
        }
        this.chunks = documentChunks;
    }

    public Document chunks(Set<DocumentChunk> documentChunks) {
        this.setChunks(documentChunks);
        return this;
    }

    public Document addChunk(DocumentChunk documentChunk) {
        this.chunks.add(documentChunk);
        documentChunk.setDocument(this);
        return this;
    }

    public Document removeChunk(DocumentChunk documentChunk) {
        this.chunks.remove(documentChunk);
        documentChunk.setDocument(null);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document)) {
            return false;
        }
        return getId() != null && getId().equals(((Document) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Document{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", filename='" + getFilename() + "'" +
            ", contentType='" + getContentType() + "'" +
            ", fileSize=" + getFileSize() +
            ", filePath='" + getFilePath() + "'" +
            ", status='" + getStatus() + "'" +
            ", summary='" + getSummary() + "'" +
            "}";
    }
}