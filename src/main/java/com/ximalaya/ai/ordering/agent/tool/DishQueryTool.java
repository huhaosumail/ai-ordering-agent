package com.ximalaya.ai.ordering.agent.tool;

import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DishQueryTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(DishQueryTool.class);

    private final DishRepository dishRepository;

    public DishQueryTool(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Override
    public String getName() {
        return "query_dishes";
    }

    @Override
    public String getDescription() {
        return "查询菜品信息，支持按名称搜索、按分类筛选、查询所有可用菜品";
    }

    @Override
    public Mono<String> execute(Map<String, Object> parameters) {
        String keyword = (String) parameters.get("keyword");
        String category = (String) parameters.get("category");
        String available = (String) parameters.get("available");

        log.info("执行菜品查询工具: keyword={}, category={}, available={}", keyword, category, available);

        return dishRepository.findByIsAvailableTrue()
                .filter(dish -> {
                    if (keyword != null && !keyword.isEmpty()) {
                        String desc = dish.getDescription();
                        return dish.getName().contains(keyword)
                                || (desc != null && desc.contains(keyword));
                    }
                    return true;
                })
                .filter(dish -> {
                    if (category != null && !category.isEmpty()) {
                        return dish.getCategory().equals(category);
                    }
                    return true;
                })
                .collectList()
                .map(this::formatResult);
    }

    private String formatResult(List<Dish> dishes) {
        if (dishes.isEmpty()) {
            return "未找到符合条件的菜品";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(dishes.size()).append(" 道菜品：\n");
        for (Dish dish : dishes) {
            sb.append("- ").append(dish.getName())
              .append(" (").append(dish.getCategory()).append(")")
              .append(" - ¥").append(dish.getPrice())
              .append("\n  描述：").append(dish.getDescription())
              .append("\n");
        }
        return sb.toString();
    }

    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "string (可选，菜品名称关键词)");
        params.put("category", "string (可选，分类名称)");
        params.put("available", "boolean (可选，是否只查询可用菜品)");
        return params;
    }
}