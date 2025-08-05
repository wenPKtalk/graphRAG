package com.topsion.rag.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    @ConditionalOnProperty(name = "application.openai.api-key")
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }
        return new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
    }
}