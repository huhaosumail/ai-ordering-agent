package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import com.ximalaya.ai.ordering.dto.response.ApiResponse;
import com.ximalaya.ai.ordering.service.AiOrderingService;
import com.ximalaya.ai.ordering.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiOrderingController {

    private static final Logger log = LoggerFactory.getLogger(AiOrderingController.class);

    private final AiOrderingService aiOrderingService;
    private final OrderService orderService;

    public AiOrderingController(AiOrderingService aiOrderingService, OrderService orderService) {
        this.aiOrderingService = aiOrderingService;
        this.orderService = orderService;
    }

    @PostMapping("/order/parse")
    public Mono<ResponseEntity<ApiResponse<OrderRequest>>> parseOrder(@RequestBody Map<String, String> request) {
        String input = request.get("input");
        log.info("AI解析点餐请求: {}", input);
        
        return aiOrderingService.parseNaturalLanguage(input)
                .map(orderRequest -> ResponseEntity.ok(ApiResponse.success(orderRequest)));
    }

    @PostMapping("/order")
    public Mono<ResponseEntity<ApiResponse<?>>> createOrderWithAI(@RequestBody Map<String, Object> request) {
        String input = (String) request.get("input");
        Long userId = request.get("userId") != null ? ((Number) request.get("userId")).longValue() : null;
        String tableNo = (String) request.get("tableNo");
        
        log.info("AI智能点餐: input={}, userId={}, tableNo={}", input, userId, tableNo);
        
        return aiOrderingService.parseNaturalLanguage(input)
                .flatMap(orderRequest -> {
                    if (userId != null) {
                        orderRequest.setUserId(userId);
                    }
                    if (tableNo != null) {
                        orderRequest.setTableNo(tableNo);
                    }
                    return orderService.createOrder(orderRequest);
                })
                .map(order -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("AI订单创建成功", order)));
    }

    @GetMapping("/recommend")
    public Mono<ResponseEntity<ApiResponse<String>>> getRecommendation(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String preferences) {
        log.info("获取AI推荐, userId={}, preferences={}", userId, preferences);
        
        return aiOrderingService.getDishRecommendation(userId, preferences)
                .map(recommendation -> ResponseEntity.ok(ApiResponse.success(recommendation)));
    }
}