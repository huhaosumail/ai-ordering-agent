package com.ximalaya.ai.ordering.agent.tool;

import com.ximalaya.ai.ordering.entity.Order;
import com.ximalaya.ai.ordering.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderQueryTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryTool.class);

    private final OrderRepository orderRepository;

    public OrderQueryTool(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public String getName() {
        return "query_orders";
    }

    @Override
    public String getDescription() {
        return "查询订单信息，支持按用户ID、订单状态、订单号查询";
    }

    @Override
    public Mono<String> execute(Map<String, Object> parameters) {
        String orderNo = (String) parameters.get("orderNo");
        Long userId = parameters.get("userId") != null ? ((Number) parameters.get("userId")).longValue() : null;
        String status = (String) parameters.get("status");

        log.info("执行订单查询工具: orderNo={}, userId={}, status={}", orderNo, userId, status);

        return orderRepository.findAll()
                .filter(order -> {
                    if (orderNo != null && !orderNo.isEmpty()) {
                        return order.getOrderNo().contains(orderNo);
                    }
                    return true;
                })
                .filter(order -> {
                    if (userId != null) {
                        return userId.equals(order.getUserId());
                    }
                    return true;
                })
                .filter(order -> {
                    if (status != null && !status.isEmpty()) {
                        return status.equals(order.getStatus());
                    }
                    return true;
                })
                .collectList()
                .map(this::formatResult);
    }

    private String formatResult(List<Order> orders) {
        if (orders.isEmpty()) {
            return "未找到符合条件的订单";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(orders.size()).append(" 个订单：\n");
        for (Order order : orders) {
            sb.append("- 订单号：").append(order.getOrderNo())
              .append("\n  状态：").append(order.getStatus())
              .append("\n  用户ID：").append(order.getUserId())
              .append("\n  桌号：").append(order.getTableNo())
              .append("\n  金额：¥").append(order.getTotalAmount())
              .append("\n");
        }
        return sb.toString();
    }

    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("orderNo", "string (可选，订单号)");
        params.put("userId", "number (可选，用户ID)");
        params.put("status", "string (可选，订单状态：PENDING/PREPARING/SERVED/CANCELLED)");
        return params;
    }
}