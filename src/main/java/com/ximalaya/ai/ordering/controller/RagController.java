package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.dto.response.ApiResponse;
import com.ximalaya.ai.ordering.config.EmbeddingProperties;
import com.ximalaya.ai.ordering.service.DishVectorIndexService;
import com.ximalaya.ai.ordering.service.EmbeddingService;
import com.ximalaya.ai.ordering.service.RagService;
import com.ximalaya.ai.ordering.service.VectorStoreService;
import com.ximalaya.ai.ordering.vector.ScoredDocument;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;
    private final DishVectorIndexService dishVectorIndexService;
    private final VectorStoreService vectorStoreService;
    private final EmbeddingProperties embeddingProperties;

    public RagController(RagService ragService,
                         DishVectorIndexService dishVectorIndexService,
                         VectorStoreService vectorStoreService,
                         EmbeddingService embeddingService) {
        this.ragService = ragService;
        this.dishVectorIndexService = dishVectorIndexService;
        this.vectorStoreService = vectorStoreService;
        this.embeddingProperties = embeddingService.getProperties();
    }

    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<List<ScoredDocument>>>> search(@RequestParam String q) {
        return ragService.retrieve(q)
                .map(docs -> ResponseEntity.ok(ApiResponse.success(docs)));
    }

    @GetMapping("/search/text")
    public Mono<ResponseEntity<ApiResponse<String>>> searchText(@RequestParam String q) {
        return ragService.formatSearchResult(q)
                .map(text -> ResponseEntity.ok(ApiResponse.success(text)));
    }

    @PostMapping("/reindex")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> reindex() {
        return dishVectorIndexService.reindexAll()
                .then(vectorStoreService.count())
                .map(count -> ResponseEntity.ok(ApiResponse.success(Map.of(
                        "message", "向量索引重建完成",
                        "indexedCount", count
                ))));
    }

    @GetMapping("/status")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> status() {
        return vectorStoreService.count()
                .map(count -> ResponseEntity.ok(ApiResponse.success(Map.of(
                        "enabled", ragService.isEnabled(),
                        "indexedCount", count,
                        "embeddingProvider", embeddingProperties.getProvider(),
                        "embeddingEndpoint", embeddingProperties.getModel(),
                        "embeddingConfigured", embeddingProperties.isConfigured(),
                        "vectorStore", "h2-dish_embedding+memory"
                ))));
    }
}
