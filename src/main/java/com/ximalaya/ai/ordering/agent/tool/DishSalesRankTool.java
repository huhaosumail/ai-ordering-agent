package com.ximalaya.ai.ordering.agent.tool;

import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 按销量降序查询可用菜品排行
 */
@Component
public class DishSalesRankTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(DishSalesRankTool.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final DishRepository dishRepository;

    public DishSalesRankTool(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Override
    public String getName() {
        return "query_dishes_sales_rank";
    }

    @Override
    public String getDescription() {
        return "查询销量排行榜，按销量从高到低列出可用菜品";
    }

    @Override
    public Mono<String> execute(Map<String, Object> parameters) {
        int limit = parseLimit(parameters.get("limit"));
        log.info("执行销量排行工具: limit={}", limit);

        return dishRepository.findTopSales(limit)
                .collectList()
                .map(this::formatResult);
    }

    private int parseLimit(Object limitObj) {
        if (limitObj == null) {
            return DEFAULT_LIMIT;
        }
        int limit = DEFAULT_LIMIT;
        if (limitObj instanceof Number number) {
            limit = number.intValue();
        } else {
            try {
                limit = Integer.parseInt(limitObj.toString().trim());
            } catch (NumberFormatException ignored) {
                return DEFAULT_LIMIT;
            }
        }
        return Math.min(MAX_LIMIT, Math.max(1, limit));
    }

    private String formatResult(List<Dish> dishes) {
        if (dishes.isEmpty()) {
            return "暂无可用菜品的销量数据";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("销量排行榜（共 ").append(dishes.size()).append(" 道，按销量降序）：\n");
        for (int i = 0; i < dishes.size(); i++) {
            Dish dish = dishes.get(i);
            sb.append(i + 1).append(". ").append(dish.getName())
                    .append("（").append(dish.getCategory()).append("）")
                    .append(" — 销量 ").append(dish.getSalesCount() != null ? dish.getSalesCount() : 0)
                    .append(" 份，¥").append(dish.getPrice())
                    .append("\n");
        }
        return sb.toString();
    }
}
