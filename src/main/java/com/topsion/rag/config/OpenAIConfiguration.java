package com.topsion.rag.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class OpenAIConfiguration {

    @Value("${application.openai.api-key:}")
    private String apiKey;

    @Value("${application.openai.timeout:60}")
    private Integer timeoutSeconds;

    @Bean
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured. Please set application.openai.api-key property.");
        }
        return new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
    }
}