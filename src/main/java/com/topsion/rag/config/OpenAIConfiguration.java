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

    @Value("${application.openai.api-url:https://api.openai.com}")
    private String apiUrl;

    @Value("${application.openai.timeout:60}")
    private Integer timeoutSeconds;
    
    @Bean
    @ConditionalOnProperty(name = "application.openai.api-key")
    public OpenAiService openAiService() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }
        
        // Note: The OpenAiService constructor doesn't directly support custom API URLs
        // For custom API URLs, you would need to implement a custom client
        // This would require additional dependencies and configuration
        // For now, we're using the standard OpenAI API endpoint
        // If you need to use Azure OpenAI or other endpoints, you'll need to extend this configuration
        
        return new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
    }
}