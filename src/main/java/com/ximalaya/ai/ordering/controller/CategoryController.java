
package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.dto.response.ApiResponse;
import com.ximalaya.ai.ordering.entity.Category;
import com.ximalaya.ai.ordering.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        log.info("查询所有分类");
        List<Category> categories = categoryRepository.findByOrderBySortOrderAsc();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getCategoryById(@PathVariable Long id) {
        log.info("查询分类详情, id={}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在: " + id));
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        log.info("创建分类, name={}", category.getName());
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success("分类创建成功", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long id,
            @RequestBody Category category) {
        log.info("更新分类, id={}", id);
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在: " + id));

        if (category.getName() != null) existing.setName(category.getName());
        if (category.getDescription() != null) existing.setDescription(category.getDescription());
        if (category.getSortOrder() != null) existing.setSortOrder(category.getSortOrder());
        if (category.getIconUrl() != null) existing.setIconUrl(category.getIconUrl());

        Category saved = categoryRepository.save(existing);
        return ResponseEntity.ok(ApiResponse.success("分类更新成功", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        log.info("删除分类, id={}", id);
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("分类不存在: " + id);
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("分类删除成功", null));
    }
}