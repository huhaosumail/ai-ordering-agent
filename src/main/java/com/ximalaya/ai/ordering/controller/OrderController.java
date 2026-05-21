package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import com.ximalaya.ai.ordering.dto.response.ApiResponse;
import com.ximalaya.ai.ordering.dto.response.OrderResponse;
import com.ximalaya.ai.ordering.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<OrderResponse>>> createOrder(@Valid @RequestBody Mono<OrderRequest> requestMono) {
        return requestMono
                .doOnNext(request -> log.info("创建订单, 用户ID: {}, 桌号: {}", request.getUserId(), request.getTableNo()))
                .flatMap(orderService::createOrder)
                .map(order -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("订单创建成功", order)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<OrderResponse>>> getOrderById(@PathVariable Long id) {
        log.info("查询订单详情, id={}", id);
        return orderService.getOrderById(id)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)));
    }

    @GetMapping("/no/{orderNo}")
    public Mono<ResponseEntity<ApiResponse<OrderResponse>>> getOrderByNo(@PathVariable String orderNo) {
        log.info("查询订单详情, orderNo={}", orderNo);
        return orderService.getOrderByNo(orderNo)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)));
    }

    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<ApiResponse<?>>> getOrdersByUserId(@PathVariable Long userId) {
        log.info("查询用户订单, userId={}", userId);
        return orderService.getOrdersByUserId(userId)
                .collectList()
                .map(orders -> ResponseEntity.ok(ApiResponse.success(orders)));
    }

    @GetMapping("/status/{status}")
    public Mono<ResponseEntity<ApiResponse<?>>> getOrdersByStatus(@PathVariable String status) {
        log.info("查询订单状态, status={}", status);
        return orderService.getOrdersByStatus(status)
                .collectList()
                .map(orders -> ResponseEntity.ok(ApiResponse.success(orders)));
    }

    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<ApiResponse<OrderResponse>>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("更新订单状态, id={}, status={}", id, status);
        return orderService.updateOrderStatus(id, status)
                .map(order -> ResponseEntity.ok(ApiResponse.success("订单状态更新成功", order)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> cancelOrder(@PathVariable Long id) {
        log.info("取消订单, id={}", id);
        return orderService.cancelOrder(id)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.success("订单已取消", null))));
    }
}