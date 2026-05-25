package com.ximalaya.ai.ordering.agent.tool;

import com.ximalaya.ai.ordering.entity.Category;
import com.ximalaya.ai.ordering.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CategoryQueryTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CategoryQueryTool.class);

    private final CategoryRepository categoryRepository;

    public CategoryQueryTool(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public String getName() {
        return "query_categories";
    }

    @Override
    public String getDescription() {
        return "查询所有菜品分类";
    }

    @Override
    public Mono<String> execute(Map<String, Object> parameters) {
        log.info("执行分类查询工具");

        return categoryRepository.findAll()
                .collectList()
                .map(this::formatResult);
    }

    private String formatResult(List<Category> categories) {
        if (categories.isEmpty()) {
            return "暂无分类";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("当前可用分类：\n");
        for (Category category : categories) {
            sb.append("- ").append(category.getName())
              .append(" (ID: ").append(category.getId()).append(")")
              .append("\n  描述：").append(category.getDescription())
              .append("\n");
        }
        return sb.toString();
    }

    public Map<String, Object> getParameters() {
        return new HashMap<>();
    }
}