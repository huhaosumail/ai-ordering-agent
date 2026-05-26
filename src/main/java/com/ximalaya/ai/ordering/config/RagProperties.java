package com.ximalaya.ai.ordering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;
    private int topK = 5;
    private double minScore = 0.35;
    /**
     * 是否在 Agent 对话前自动注入 RAG 检索上下文
     */
    private boolean injectToAgentPrompt = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public boolean isInjectToAgentPrompt() {
        return injectToAgentPrompt;
    }

    public void setInjectToAgentPrompt(boolean injectToAgentPrompt) {
        this.injectToAgentPrompt = injectToAgentPrompt;
    }
}
