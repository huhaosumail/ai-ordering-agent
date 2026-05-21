package com.ximalaya.ai.ordering.service.impl;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import com.ximalaya.ai.ordering.dto.response.OrderResponse;
import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.entity.Order;
import com.ximalaya.ai.ordering.repository.DishRepository;
import com.ximalaya.ai.ordering.repository.OrderRepository;
import com.ximalaya.ai.ordering.service.OrderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;

    public OrderServiceImpl(OrderRepository orderRepository, DishRepository dishRepository) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
    }

    @Override
    public Mono<OrderResponse> createOrder(OrderRequest request) {
        log.debug("创建订单, 用户ID: {}, 桌号: {}", request.getUserId(), request.getTableNo());

        return Flux.fromIterable(request.getItems())
                .flatMap(item -> dishRepository.findById(item.getDishId())
                        .switchIfEmpty(Mono.error(new RuntimeException("菜品不存在: " + item.getDishId())))
                        .filter(Dish::getIsAvailable)
                        .switchIfEmpty(Mono.error(new RuntimeException("菜品不可用"))))
                .collectList()
                .flatMap(dishes -> {
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    List<OrderResponse.OrderItemResponse> items = new ArrayList<>();

                    for (int i = 0; i < dishes.size(); i++) {
                        Dish dish = dishes.get(i);
                        int quantity = request.getItems().get(i).getQuantity();
                        BigDecimal subtotal = dish.getPrice().multiply(BigDecimal.valueOf(quantity));
                        totalAmount = totalAmount.add(subtotal);

                        items.add(OrderResponse.OrderItemResponse.builder()
                                .dishId(dish.getId())
                                .dishName(dish.getName())
                                .price(dish.getPrice())
                                .quantity(quantity)
                                .subtotal(subtotal)
                                .build());

                        dish.setSalesCount(dish.getSalesCount() + quantity);
                    }

                    String orderNo = generateOrderNo();

                    Order order = Order.builder()
                            .orderNo(orderNo)
                            .userId(request.getUserId())
                            .tableNo(request.getTableNo())
                            .status("PENDING")
                            .totalAmount(totalAmount)
                            .items(toJson(items))
                            .remark(request.getRemark())
                            .createdAt(LocalDateTime.now())
                            .build();

                    return orderRepository.save(order)
                            .doOnSuccess(savedOrder -> {
                                Flux.fromIterable(dishes)
                                        .flatMap(dishRepository::save)
                                        .subscribe();
                            })
                            .map(savedOrder -> toResponse(savedOrder, items));
                })
                .doOnSuccess(response -> log.info("订单创建成功, 订单号: {}", response.getOrderNo()));
    }

    @Override
    public Mono<OrderResponse> getOrderById(Long id) {
        log.debug("获取订单 ID: {}", id);
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("订单不存在: " + id)))
                .map(order -> toResponse(order, parseItems(order.getItems())));
    }

    @Override
    public Mono<OrderResponse> getOrderByNo(String orderNo) {
        log.debug("获取订单号: {}", orderNo);
        return orderRepository.findByOrderNo(orderNo)
                .switchIfEmpty(Mono.error(new RuntimeException("订单不存在: " + orderNo)))
                .map(order -> toResponse(order, parseItems(order.getItems())));
    }

    @Override
    public Flux<OrderResponse> getOrdersByUserId(Long userId) {
        log.debug("获取用户订单, 用户ID: {}", userId);
        return orderRepository.findByUserId(userId)
                .map(order -> toResponse(order, parseItems(order.getItems())));
    }

    @Override
    public Flux<OrderResponse> getOrdersByStatus(String status) {
        log.debug("获取状态为 {} 的订单", status);
        return orderRepository.findByStatus(status)
                .map(order -> toResponse(order, parseItems(order.getItems())));
    }

    @Override
    public Mono<OrderResponse> updateOrderStatus(Long id, String status) {
        log.debug("更新订单状态, 订单ID: {}, 状态: {}", id, status);
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("订单不存在: " + id)))
                .flatMap(order -> {
                    order.setStatus(status);
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .map(order -> toResponse(order, parseItems(order.getItems())));
    }

    @Override
    public Mono<Void> cancelOrder(Long id) {
        log.debug("取消订单, 订单ID: {}", id);
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("订单不存在: " + id)))
                .filter(order -> "PENDING".equals(order.getStatus()))
                .switchIfEmpty(Mono.error(new RuntimeException("只能取消待支付的订单")))
                .flatMap(order -> {
                    order.setStatus("CANCELLED");
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order).then();
                });
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private String toJson(List<OrderResponse.OrderItemResponse> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<OrderResponse.OrderItemResponse> parseItems(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<OrderResponse.OrderItemResponse>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private OrderResponse toResponse(Order order, List<OrderResponse.OrderItemResponse> items) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .tableNo(order.getTableNo())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}