package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import reactor.core.publisher.Mono;

public interface AiOrderingService {

    Mono<OrderRequest> parseNaturalLanguage(String input);

    Mono<String> getDishRecommendation(Long userId, String preferences);
}