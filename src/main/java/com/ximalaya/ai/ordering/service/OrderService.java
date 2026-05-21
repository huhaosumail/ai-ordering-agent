package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import com.ximalaya.ai.ordering.dto.response.OrderResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {

    Mono<OrderResponse> createOrder(OrderRequest request);

    Mono<OrderResponse> getOrderById(Long id);

    Mono<OrderResponse> getOrderByNo(String orderNo);

    Flux<OrderResponse> getOrdersByUserId(Long userId);

    Flux<OrderResponse> getOrdersByStatus(String status);

    Mono<OrderResponse> updateOrderStatus(Long id, String status);

    Mono<Void> cancelOrder(Long id);
}