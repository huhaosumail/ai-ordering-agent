package com.ximalaya.ai.ordering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "eval")
public class EvalProperties {

    private boolean enabled = true;

    /** 跑 RAG 评估前是否先 reindex（保证向量与菜品一致） */
    private boolean reindexBeforeRag = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isReindexBeforeRag() {
        return reindexBeforeRag;
    }

    public void setReindexBeforeRag(boolean reindexBeforeRag) {
        this.reindexBeforeRag = reindexBeforeRag;
    }
}
