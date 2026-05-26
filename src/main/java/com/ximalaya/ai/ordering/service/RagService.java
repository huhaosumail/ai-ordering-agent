package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.config.RagProperties;
import com.ximalaya.ai.ordering.vector.ScoredDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final RagProperties ragProperties;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public RagService(RagProperties ragProperties,
                      EmbeddingService embeddingService,
                      VectorStoreService vectorStoreService) {
        this.ragProperties = ragProperties;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    public boolean isEnabled() {
        return ragProperties.isEnabled();
    }

    public Mono<List<ScoredDocument>> retrieve(String query) {
        if (!ragProperties.isEnabled() || query == null || query.isBlank()) {
            return Mono.just(List.of());
        }
        return embeddingService.embed(query)
                .flatMap(queryVector -> vectorStoreService.similaritySearch(
                        queryVector,
                        ragProperties.getTopK(),
                        ragProperties.getMinScore()))
                .doOnNext(docs -> log.debug("RAG 检索 query='{}' 命中 {} 条", query, docs.size()));
    }

    public Mono<String> buildContextBlock(String query) {
        return retrieve(query)
                .map(this::formatContext);
    }

    /** Agent 对话前注入的 RAG 上下文（受 inject-to-agent-prompt 开关控制） */
    public Mono<String> buildAgentContext(String query) {
        if (!ragProperties.isEnabled() || !ragProperties.isInjectToAgentPrompt()) {
            return Mono.just("");
        }
        return buildContextBlock(query);
    }

    public Mono<String> formatSearchResult(String query) {
        return retrieve(query)
                .map(docs -> {
                    if (docs.isEmpty()) {
                        return "未找到与「" + query + "」语义相近的菜品，可尝试换种描述或指定菜名。";
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("语义检索「").append(query).append("」共 ").append(docs.size()).append(" 道相关菜品：\n\n");
                    int i = 1;
                    for (ScoredDocument doc : docs) {
                        sb.append(i++).append(". ").append(doc.dishName())
                                .append("（").append(doc.category()).append("）")
                                .append(" — 相关度 ").append(String.format("%.2f", doc.score())).append("\n");
                        sb.append("   ").append(extractDescription(doc.content())).append("\n");
                    }
                    return sb.toString();
                });
    }

    private String formatContext(List<ScoredDocument> docs) {
        if (docs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【RAG 检索到的相关菜品（供回答参考，可结合工具进一步查询/下单）】\n");
        for (ScoredDocument doc : docs) {
            sb.append("- ").append(doc.dishName())
                    .append(" | ").append(doc.category())
                    .append(" | 相关度 ").append(String.format("%.2f", doc.score()))
                    .append("\n  ").append(extractDescription(doc.content())).append("\n");
        }
        return sb.toString();
    }

    private String extractDescription(String content) {
        for (String line : content.split("\n")) {
            if (line.startsWith("描述:")) {
                return line.substring(3).trim();
            }
        }
        return content;
    }
}
