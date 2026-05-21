package com.ximalaya.ai.ordering.service.impl;

import com.ximalaya.ai.ordering.dto.request.DishRequest;
import com.ximalaya.ai.ordering.dto.response.DishResponse;
import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.repository.DishRepository;
import com.ximalaya.ai.ordering.service.DishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class DishServiceImpl implements DishService {

    private static final Logger log = LoggerFactory.getLogger(DishServiceImpl.class);

    private final DishRepository dishRepository;

    public DishServiceImpl(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Override
    public Flux<DishResponse> getAvailableDishes() {
        log.debug("查询所有可用菜品");
        return dishRepository.findByIsAvailableTrue()
                .map(this::toResponse)
                .doOnNext(dish -> log.debug("菜品: {}", dish.getName()));
    }

    @Override
    public Mono<DishResponse> getDishById(Long id) {
        log.debug("查询菜品ID: {}", id);
        return dishRepository.findById(id)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new RuntimeException("菜品不存在: " + id)));
    }

    @Override
    public Flux<DishResponse> getDishesByCategory(String category) {
        log.debug("查询分类菜品, category={}", category);
        return dishRepository.findByCategory(category)
                .filter(Dish::getIsAvailable)
                .map(this::toResponse);
    }

    @Override
    public Flux<DishResponse> searchDishes(String keyword) {
        log.debug("搜索菜品, keyword={}", keyword);
        String likeKeyword = "%" + keyword + "%";
        return dishRepository.searchByKeyword(likeKeyword)
                .map(this::toResponse);
    }

    @Override
    public Flux<DishResponse> getTopSales() {
        log.debug("查询销量最高的菜品");
        return dishRepository.findTopSales(10)
                .map(this::toResponse);
    }

    @Override
    public Flux<DishResponse> getTopRated() {
        log.debug("查询评分最高的菜品");
        return dishRepository.findTopRated(10)
                .map(this::toResponse);
    }

    @Override
    public Mono<DishResponse> createDish(DishRequest request) {
        log.info("创建菜品, name={}", request.getName());
        BigDecimal rating = request.getRating() != null ? 
                BigDecimal.valueOf(request.getRating()) : BigDecimal.ZERO;
        
        Dish dish = Dish.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .isAvailable(true)
                .salesCount(0)
                .rating(rating)
                .ratingCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return dishRepository.save(dish)
                .map(this::toResponse)
                .doOnSuccess(response -> log.info("菜品创建成功, id={}", response.getId()));
    }

    @Override
    public Mono<DishResponse> updateDish(Long id, DishRequest request) {
        log.info("更新菜品, id={}", id);
        return dishRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("菜品不存在: " + id)))
                .flatMap(existing -> {
                    existing.setName(request.getName());
                    existing.setDescription(request.getDescription());
                    existing.setPrice(request.getPrice());
                    existing.setCategory(request.getCategory());
                    existing.setImageUrl(request.getImageUrl());
                    existing.setIsAvailable(request.getIsAvailable());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return dishRepository.save(existing);
                })
                .map(this::toResponse);
    }

    @Override
    public Mono<Void> deleteDish(Long id) {
        log.info("删除菜品, id={}", id);
        return dishRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("菜品不存在: " + id)))
                .flatMap(dishRepository::delete);
    }

    private DishResponse toResponse(Dish dish) {
        return DishResponse.builder()
                .id(dish.getId())
                .name(dish.getName())
                .description(dish.getDescription())
                .price(dish.getPrice())
                .category(dish.getCategory())
                .imageUrl(dish.getImageUrl())
                .isAvailable(dish.getIsAvailable())
                .salesCount(dish.getSalesCount())
                .rating(dish.getRating() != null ? dish.getRating().doubleValue() : 0.0)
                .ratingCount(dish.getRatingCount())
                .createdAt(dish.getCreatedAt())
                .updatedAt(dish.getUpdatedAt())
                .build();
    }
}