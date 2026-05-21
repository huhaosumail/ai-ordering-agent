
package com.ximalaya.ai.ordering.controller;

import com.ximalaya.ai.ordering.dto.request.DishRequest;
import com.ximalaya.ai.ordering.dto.response.ApiResponse;
import com.ximalaya.ai.ordering.dto.response.DishResponse;
import com.ximalaya.ai.ordering.service.DishService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private static final Logger log = LoggerFactory.getLogger(DishController.class);

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DishResponse>>> getAllDishes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        log.info("查询菜品列表, category={}, keyword={}", category, keyword);
        List<DishResponse> dishes;

        if (keyword != null && !keyword.isEmpty()) {
            dishes = dishService.searchDishes(keyword);
        } else if (category != null && !category.isEmpty()) {
            dishes = dishService.getDishesByCategory(category);
        } else {
            dishes = dishService.getAvailableDishes();
        }

        return ResponseEntity.ok(ApiResponse.success(dishes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DishResponse>> getDishById(@PathVariable Long id) {
        log.info("查询菜品详情, id={}", id);
        DishResponse dish = dishService.getDishById(id);
        return ResponseEntity.ok(ApiResponse.success(dish));
    }

    @GetMapping("/top-sales")
    public ResponseEntity<ApiResponse<List<DishResponse>>> getTopSales() {
        log.info("查询销量最高的菜品");
        List<DishResponse> dishes = dishService.getTopSales();
        return ResponseEntity.ok(ApiResponse.success(dishes));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<List<DishResponse>>> getTopRated() {
        log.info("查询评分最高的菜品");
        List<DishResponse> dishes = dishService.getTopRated();
        return ResponseEntity.ok(ApiResponse.success(dishes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DishResponse>> createDish(@Valid @RequestBody DishRequest request) {
        log.info("创建菜品, name={}", request.getName());
        DishResponse dish = dishService.createDish(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("菜品创建成功", dish));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DishResponse>> updateDish(
            @PathVariable Long id,
            @Valid @RequestBody DishRequest request) {
        log.info("更新菜品, id={}", id);
        DishResponse dish = dishService.updateDish(id, request);
        return ResponseEntity.ok(ApiResponse.success("菜品更新成功", dish));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDish(@PathVariable Long id) {
        log.info("删除菜品, id={}", id);
        dishService.deleteDish(id);
        return ResponseEntity.ok(ApiResponse.success("菜品删除成功", null));
    }
}