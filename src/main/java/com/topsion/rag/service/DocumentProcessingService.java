package com.topsion.rag.service;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import com.topsion.rag.config.ApplicationProperties;
import com.topsion.rag.domain.Document;
import com.topsion.rag.domain.DocumentChunk;
import com.topsion.rag.repository.DocumentChunkRepository;
import com.topsion.rag.repository.DocumentRepository;
import com.topsion.rag.repository.EntityRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentProcessingService {

    private final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EntityRepository entityRepository;
    private final OpenAiService openAiService;
    private final ApplicationProperties applicationProperties;
    private final EntityExtractionService entityExtractionService;

    private final Tika tika = new Tika();

    public DocumentProcessingService(
        DocumentRepository documentRepository,
        DocumentChunkRepository documentChunkRepository,
        EntityRepository entityRepository,
        @Autowired(required = false) OpenAiService openAiService,
        ApplicationProperties applicationProperties,
        EntityExtractionService entityExtractionService
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.entityRepository = entityRepository;
        this.openAiService = openAiService;
        this.applicationProperties = applicationProperties;
        this.entityExtractionService = entityExtractionService;
    }

    public Mono<Document> uploadAndProcessDocument(FilePart filePart) {
        return saveFile(filePart)
            .flatMap(this::createDocumentEntity)
            .flatMap(this::processDocument);
    }

    private Mono<Path> saveFile(FilePart filePart) {
        return Mono.fromCallable(() -> {
            String uploadDir = "uploads/documents/";
            Files.createDirectories(Paths.get(uploadDir));

            String fileName = System.currentTimeMillis() + "_" + filePart.filename();
            Path filePath = Paths.get(uploadDir, fileName);

            return filePath;
        })
        .flatMap(filePath ->
            filePart.transferTo(filePath.toFile())
                .then(Mono.just(filePath))
        );
    }

    private Mono<Document> createDocumentEntity(Path filePath) {
        return Mono.fromCallable(() -> {
            try {
                File file = filePath.toFile();
                String contentType = tika.detect(file);

                Document document = new Document();
                document.setTitle(extractTitleFromFilename(file.getName()));
                document.setFilename(file.getName());
                document.setContentType(contentType);
                document.setFileSize(file.length());
                document.setFilePath(filePath.toString());
                document.setStatus("UPLOADED");
                document.setCreatedDate(Instant.now());

                return document;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create document entity", e);
            }
        })
        .flatMap(documentRepository::save);
    }

    private Mono<Document> processDocument(Document document) {
        return Mono.fromCallable(() -> {
            document.setStatus("PROCESSING");
            return document;
        })
        .flatMap(documentRepository::save)
        .flatMap(this::extractTextContent)
        .flatMap(this::chunkDocument)
        .flatMap(this::generateEmbeddings)
        .flatMap(this::extractEntities)
        .flatMap(doc -> {
            doc.setStatus("PROCESSED");
            return documentRepository.save(doc);
        })
        .onErrorResume(error -> {
            log.error("Error processing document: {}", error.getMessage(), error);
            document.setStatus("ERROR");
            return documentRepository.save(document);
        });
    }

    private Mono<Document> extractTextContent(Document document) {
        return Mono.fromCallable(() -> {
            try {
                String content = extractTextFromFile(Paths.get(document.getFilePath()));

                String summary = content.length() > 500
                    ? content.substring(0, 500) + "..."
                    : content;
                document.setSummary(summary);

                return document;
            } catch (Exception e) {
                throw new RuntimeException("Failed to extract text content", e);
            }
        });
    }

    private String extractTextFromFile(Path filePath) throws IOException, TikaException {
        File file = filePath.toFile();
        String contentType = tika.detect(file);

        switch (contentType) {
            case "application/pdf":
                return extractTextFromPdf(file);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return extractTextFromDocx(file);
            case "text/plain":
                return Files.readString(filePath);
            default:
                return tika.parseToString(file);
        }
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private Mono<Document> chunkDocument(Document document) {
        return Mono.fromCallable(() -> {
            String content = document.getSummary();
            if (document.getSummary().endsWith("...")) {
                try {
                    content = extractTextFromFile(Paths.get(document.getFilePath()));
                } catch (IOException | TikaException e) {
                    log.error("Failed to re-extract full text content", e);
                    content = document.getSummary();
                }
            }

            List<String> chunks = createTextChunks(content);
            AtomicInteger index = new AtomicInteger(0);

            Set<DocumentChunk> documentChunks = chunks.stream()
                .map(chunkContent -> {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setContent(chunkContent);
                    chunk.setChunkIndex(index.getAndIncrement());
                    chunk.setTokenCount(estimateTokenCount(chunkContent));
                    chunk.setDocument(document);
                    chunk.setCreatedDate(Instant.now());
                    return chunk;
                })
                .collect(Collectors.toSet());

            document.setChunks(documentChunks);
            return document;
        });
    }

    private List<String> createTextChunks(String content) {
        int chunkSize = applicationProperties.getOpenai().getRag().getChunkSize();
        int overlap = applicationProperties.getOpenai().getRag().getChunkOverlap();

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            if (end < content.length()) {
                int lastSpace = content.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            chunks.add(content.substring(start, end).trim());
            start = Math.max(start + chunkSize - overlap, end);
        }

        return chunks;
    }

    private int estimateTokenCount(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }

    private Mono<Document> generateEmbeddings(Document document) {
        return Flux.fromIterable(document.getChunks())
            .flatMap(this::generateChunkEmbedding)
            .collectList()
            .map(chunks -> {
                document.getChunks().clear();
                document.getChunks().addAll(chunks);
                return document;
            });
    }

    private Mono<DocumentChunk> generateChunkEmbedding(DocumentChunk chunk) {
        return Mono.fromCallable(() -> {
            try {
                if (openAiService == null) {
                    log.warn("OpenAI service is not configured, skipping embedding generation for chunk");
                    return chunk;
                }

                String model = applicationProperties.getOpenai().getModel().getEmbedding();
                EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(model)
                    .input(List.of(chunk.getContent()))
                    .build();

                var response = openAiService.createEmbeddings(request);
                if (!response.getData().isEmpty()) {
                    List<Double> embedding = response.getData().get(0).getEmbedding();
                    chunk.setEmbedding(embedding.stream().mapToDouble(Double::doubleValue).toArray());
                }

                return chunk;
            } catch (Exception e) {
                log.error("Failed to generate embedding for chunk: {}", e.getMessage(), e);
                return chunk;
            }
        });
    }

    private Mono<Document> extractEntities(Document document) {
        return entityExtractionService.extractEntitiesFromDocument(document)
            .then(Mono.just(document));
    }

    private String extractTitleFromFilename(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }

    public Flux<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Mono<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    public Mono<Void> deleteDocument(Long id) {
        return documentRepository.findById(id)
            .flatMap(document -> {
                try {
                    Files.deleteIfExists(Paths.get(document.getFilePath()));
                } catch (IOException e) {
                    log.error("Failed to delete file: {}", e.getMessage(), e);
                }
                return documentRepository.deleteById(id);
            });
    }
}
