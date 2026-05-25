package com.ximalaya.ai.ordering.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "agent.ordering")
public class AiConfig {

    private PromptConfig prompt = new PromptConfig();
    private MemoryConfig memory = new MemoryConfig();

    @Data
    public static class PromptConfig {
        private String system;
        private String fewShot;
    }

    @Data
    public static class MemoryConfig {
        private int maxHistoryMessages = 10;
    }
}