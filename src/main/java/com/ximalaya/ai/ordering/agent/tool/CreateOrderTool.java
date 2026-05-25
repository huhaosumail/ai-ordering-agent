package com.ximalaya.ai.ordering.agent.tool;

import com.ximalaya.ai.ordering.dto.request.OrderRequest;
import com.ximalaya.ai.ordering.dto.request.OrderRequest.OrderItem;
import com.ximalaya.ai.ordering.dto.response.OrderResponse;
import com.ximalaya.ai.ordering.repository.DishRepository;
import com.ximalaya.ai.ordering.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CreateOrderTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderTool.class);

    private final OrderService orderService;
    private final DishRepository dishRepository;

    public CreateOrderTool(OrderService orderService, DishRepository dishRepository) {
        this.orderService = orderService;
        this.dishRepository = dishRepository;
    }

    @Override
    public String getName() {
        return "create_order";
    }

    @Override
    public String getDescription() {
        return "根据菜品名称和数量创建订单";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<String> execute(Map<String, Object> parameters) {
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) parameters.get("items");
        if (rawItems == null || rawItems.isEmpty()) {
            return Mono.just("下单失败：未识别到菜品和数量");
        }

        Long userId = parameters.get("userId") != null
                ? ((Number) parameters.get("userId")).longValue() : 1L;
        String tableNo = (String) parameters.get("tableNo");
        String remark = (String) parameters.get("remark");

        log.info("执行下单工具: userId={}, items={}", userId, rawItems);

        return Flux.fromIterable(rawItems)
                .concatMap(raw -> {
                    String name = String.valueOf(raw.get("name")).trim();
                    int quantity = raw.get("quantity") instanceof Number n
                            ? n.intValue() : Integer.parseInt(String.valueOf(raw.get("quantity")));
                    return dishRepository.findByNameLike("%" + name + "%")
                            .filter(d -> d.getName().contains(name) || name.contains(d.getName()))
                            .next()
                            .switchIfEmpty(Mono.error(new RuntimeException("未找到菜品: " + name)))
                            .map(dish -> OrderItem.builder().dishId(dish.getId()).quantity(quantity).build());
                })
                .collectList()
                .flatMap(orderItems -> {
                    OrderRequest request = OrderRequest.builder()
                            .userId(userId)
                            .tableNo(tableNo)
                            .items(orderItems)
                            .remark(remark)
                            .build();
                    return orderService.createOrder(request);
                })
                .map(this::formatSuccess)
                .onErrorResume(e -> Mono.just("下单失败：" + e.getMessage()));
    }

    private String formatSuccess(OrderResponse order) {
        StringBuilder sb = new StringBuilder();
        sb.append("订单创建成功\n");
        sb.append("订单号：").append(order.getOrderNo()).append("\n");
        sb.append("状态：").append(order.getStatus()).append("\n");
        sb.append("总金额：¥").append(order.getTotalAmount()).append("\n");
        sb.append("明细：\n");
        for (OrderResponse.OrderItemResponse item : order.getItems()) {
            sb.append("- ").append(item.getDishName())
                    .append(" x").append(item.getQuantity())
                    .append(" = ¥").append(item.getSubtotal()).append("\n");
        }
        return sb.toString();
    }

    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("items", "array [{name, quantity}]");
        params.put("userId", "number (可选，默认1)");
        params.put("tableNo", "string (可选)");
        params.put("remark", "string (可选)");
        return params;
    }
}
