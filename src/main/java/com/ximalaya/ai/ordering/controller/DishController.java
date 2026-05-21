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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private static final Logger log = LoggerFactory.getLogger(DishController.class);

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<?>>> getAllDishes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        log.info("查询菜品列表, category={}, keyword={}", category, keyword);

        if (keyword != null && !keyword.isEmpty()) {
            return dishService.searchDishes(keyword)
                    .collectList()
                    .map(dishes -> ResponseEntity.ok(ApiResponse.success(dishes)));
        } else if (category != null && !category.isEmpty()) {
            return dishService.getDishesByCategory(category)
                    .collectList()
                    .map(dishes -> ResponseEntity.ok(ApiResponse.success(dishes)));
        } else {
            return dishService.getAvailableDishes()
                    .collectList()
                    .map(dishes -> ResponseEntity.ok(ApiResponse.success(dishes)));
        }
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<DishResponse>>> getDishById(@PathVariable Long id) {
        log.info("查询菜品详情, id={}", id);
        return dishService.getDishById(id)
                .map(dish -> ResponseEntity.ok(ApiResponse.success(dish)));
    }

    @GetMapping("/top-sales")
    public Mono<ResponseEntity<ApiResponse<?>>> getTopSales() {
        log.info("查询销量最高的菜品");
        return dishService.getTopSales()
                .collectList()
                .map(dishes -> ResponseEntity.ok(ApiResponse.success(dishes)));
    }

    @GetMapping("/top-rated")
    public Mono<ResponseEntity<ApiResponse<?>>> getTopRated() {
        log.info("查询评分最高的菜品");
        return dishService.getTopRated()
                .collectList()
                .map(dishes -> ResponseEntity.ok(ApiResponse.success(dishes)));
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<DishResponse>>> createDish(@Valid @RequestBody Mono<DishRequest> requestMono) {
        return requestMono
                .doOnNext(request -> log.info("创建菜品, name={}", request.getName()))
                .flatMap(dishService::createDish)
                .map(dish -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("菜品创建成功", dish)));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<DishResponse>>> updateDish(
            @PathVariable Long id,
            @Valid @RequestBody Mono<DishRequest> requestMono) {
        log.info("更新菜品, id={}", id);
        return requestMono
                .flatMap(request -> dishService.updateDish(id, request))
                .map(dish -> ResponseEntity.ok(ApiResponse.success("菜品更新成功", dish)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteDish(@PathVariable Long id) {
        log.info("删除菜品, id={}", id);
        return dishService.deleteDish(id)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.success("菜品删除成功", null))));
    }
}