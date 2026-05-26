package com.ximalaya.ai.ordering.agent.tool;

import com.ximalaya.ai.ordering.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 基于向量相似度的菜品语义检索（RAG 召回）
 */
@Component
public class SemanticDishSearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(SemanticDishSearchTool.class);

    private final RagService ragService;

    public SemanticDishSearchTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String getName() {
        return "semantic_search_dishes";
    }

    @Override
    public String getDescription() {
        return "按语义/口味/场景搜索菜品（向量检索），适合「辣的」「下饭」「清淡」「约会」等模糊描述";
    }

    @Override
    public Mono<String> execute(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        if (query == null || query.isBlank()) {
            query = (String) parameters.get("keyword");
        }
        if (query == null || query.isBlank()) {
            return Mono.just("请提供 query 参数，描述你想找的菜品或口味。");
        }
        if (!ragService.isEnabled()) {
            return Mono.just("语义检索未启用，请使用 query_dishes 按关键词查询。");
        }
        log.info("执行语义菜品检索: query={}", query);
        return ragService.formatSearchResult(query.trim());
    }
}
