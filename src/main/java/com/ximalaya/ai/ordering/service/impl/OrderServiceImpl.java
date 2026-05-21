
package com.ximalaya.ai.ordering.service.impl;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import com.ximalaya.ai.ordering.dto.response.OrderResponse;
import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.entity.Order;
import com.ximalaya.ai.ordering.repository.DishRepository;
import com.ximalaya.ai.ordering.repository.OrderRepository;
import com.ximalaya.ai.ordering.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;

    public OrderServiceImpl(OrderRepository orderRepository, DishRepository dishRepository) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.debug("创建订单, 用户ID: {}, 桌号: {}", request.getUserId(), request.getTableNo());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderResponse.OrderItemResponse> items = new ArrayList<>();

        for (OrderRequest.OrderItem item : request.getItems()) {
            Dish dish = dishRepository.findById(item.getDishId())
                    .orElseThrow(() -> new RuntimeException("菜品不存在: " + item.getDishId()));

            if (!dish.getIsAvailable()) {
                throw new RuntimeException("菜品不可用: " + dish.getName());
            }

            BigDecimal subtotal = dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            items.add(OrderResponse.OrderItemResponse.builder()
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .price(dish.getPrice())
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .build());

            dish.setSalesCount(dish.getSalesCount() + item.getQuantity());
            dishRepository.save(dish);
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

        order = orderRepository.save(order);
        log.info("订单创建成功, 订单号: {}", orderNo);

        return toResponse(order, items);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        log.debug("获取订单 ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + id));
        return toResponse(order, parseItems(order.getItems()));
    }

    @Override
    public OrderResponse getOrderByNo(String orderNo) {
        log.debug("获取订单号: {}", orderNo);
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNo));
        return toResponse(order, parseItems(order.getItems()));
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.debug("获取用户订单, 用户ID: {}", userId);
        return orderRepository.findByUserId(userId).stream()
                .map(order -> toResponse(order, parseItems(order.getItems())))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(String status) {
        log.debug("获取状态为 {} 的订单", status);
        return orderRepository.findByStatus(status).stream()
                .map(order -> toResponse(order, parseItems(order.getItems())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, String status) {
        log.debug("更新订单状态, 订单ID: {}, 状态: {}", id, status);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + id));

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        return toResponse(order, parseItems(order.getItems()));
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        log.debug("取消订单, 订单ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + id));

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("只能取消待支付的订单");
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private String toJson(List<OrderResponse.OrderItemResponse> items) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(items);
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
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json,
                            new com.fasterxml.jackson.core.type.TypeReference<List<OrderResponse.OrderItemResponse>>() {});
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