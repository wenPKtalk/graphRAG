package com.topsion.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Y.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {
    private final OpenAI openai = new OpenAI();

    public OpenAI getOpenai() {
        return openai;
    }

    public static class OpenAI {
        private String apiKey;
        private String apiUrl = "https://api.openai.com";
        private Integer timeout = 60;
        private final Model model = new Model();
        private final Rag rag = new Rag();

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Model getModel() {
            return model;
        }

        public Rag getRag() {
            return rag;
        }

        public static class Model {
            private String chat = "gpt-3.5-turbo";
            private String embedding = "text-embedding-ada-002";

            public String getChat() {
                return chat;
            }

            public void setChat(String chat) {
                this.chat = chat;
            }

            public String getEmbedding() {
                return embedding;
            }

            public void setEmbedding(String embedding) {
                this.embedding = embedding;
            }
        }

        public static class Rag {
            private Integer chunkSize = 1000;
            private Integer chunkOverlap = 200;
            private Integer maxContextChunks = 5;
            private Double similarityThreshold = 0.7;

            public Integer getChunkSize() {
                return chunkSize;
            }

            public void setChunkSize(Integer chunkSize) {
                this.chunkSize = chunkSize;
            }

            public Integer getChunkOverlap() {
                return chunkOverlap;
            }

            public void setChunkOverlap(Integer chunkOverlap) {
                this.chunkOverlap = chunkOverlap;
            }

            public Integer getMaxContextChunks() {
                return maxContextChunks;
            }

            public void setMaxContextChunks(Integer maxContextChunks) {
                this.maxContextChunks = maxContextChunks;
            }

            public Double getSimilarityThreshold() {
                return similarityThreshold;
            }

            public void setSimilarityThreshold(Double similarityThreshold) {
                this.similarityThreshold = similarityThreshold;
            }
        }
    }
}
