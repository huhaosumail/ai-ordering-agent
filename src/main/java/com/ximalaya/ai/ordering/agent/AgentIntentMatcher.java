package com.ximalaya.ai.ordering.agent;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 模拟模式下 Agent 工具路由（与 {@link com.ximalaya.ai.ordering.agent.impl.AgentServiceImpl} 共用，供评估复用）
 */
@Component
public class AgentIntentMatcher {

    public record MatchedIntent(String toolName, Map<String, Object> params) {}

    private final OrderIntentParser orderIntentParser;

    public AgentIntentMatcher(OrderIntentParser orderIntentParser) {
        this.orderIntentParser = orderIntentParser;
    }

    /**
     * @return 若无法匹配工具则 empty（应对话兜底文案）
     */
    public Optional<MatchedIntent> match(String userMsg, Long userId) {
        if (userMsg == null || userMsg.isBlank()) {
            return Optional.empty();
        }
        String msg = userMsg.trim();

        Optional<Map<String, Object>> order = orderIntentParser.tryParse(msg, userId);
        if (order.isPresent()) {
            return Optional.of(new MatchedIntent("create_order", order.get()));
        }
        if (msg.contains("辣") || msg.contains("麻辣") || msg.contains("推荐")
                || msg.contains("好吃") || msg.contains("口味")) {
            return Optional.of(new MatchedIntent("semantic_search_dishes", Map.of("query", msg)));
        }
        if (msg.contains("宫保鸡丁") || msg.contains("鸡丁")) {
            return Optional.of(new MatchedIntent("query_dishes", Map.of("keyword", "宫保鸡丁")));
        }
        if (msg.contains("订单") || msg.contains("订了") || msg.contains("购买")) {
            return Optional.of(new MatchedIntent("query_orders", Map.of()));
        }
        if (msg.contains("分类") || msg.contains("种类")) {
            return Optional.of(new MatchedIntent("query_categories", Map.of()));
        }
        if (containsSalesRankIntent(msg)) {
            return Optional.of(new MatchedIntent("query_dishes_sales_rank", Map.of()));
        }
        if (containsDishQueryIntent(msg)) {
            return Optional.of(new MatchedIntent("query_dishes", Map.of()));
        }
        return Optional.empty();
    }

    private boolean containsSalesRankIntent(String userMsg) {
        return userMsg.contains("销量") || userMsg.contains("畅销") || userMsg.contains("卖得最好")
                || userMsg.contains("卖得好") || (userMsg.contains("排行") && userMsg.contains("榜"));
    }

    private boolean containsDishQueryIntent(String userMsg) {
        return userMsg.contains("菜单") || userMsg.contains("好吃") || userMsg.contains("推荐")
                || userMsg.contains("有什么") || userMsg.contains("有哪些");
    }
}
