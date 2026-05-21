
package com.ximalaya.ai.ordering.service.impl;

import com.ximalaya.ai.ordering.dto.request.DishRequest;
import com.ximalaya.ai.ordering.dto.response.DishResponse;
import com.ximalaya.ai.ordering.entity.Dish;
import com.ximalaya.ai.ordering.repository.DishRepository;
import com.ximalaya.ai.ordering.service.DishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl implements DishService {

    private static final Logger log = LoggerFactory.getLogger(DishServiceImpl.class);

    private final DishRepository dishRepository;

    public DishServiceImpl(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Override
    public List<DishResponse> getAllDishes() {
        log.debug("获取所有菜品");
        return dishRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DishResponse> getAvailableDishes() {
        log.debug("获取可用菜品");
        return dishRepository.findByIsAvailableTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DishResponse> getDishesByCategory(String category) {
        log.debug("获取分类 {} 的菜品", category);
        return dishRepository.findByCategoryAndIsAvailableTrue(category).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DishResponse getDishById(Long id) {
        log.debug("获取菜品 ID: {}", id);
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("菜品不存在: " + id));
        return toResponse(dish);
    }

    @Override
    @Transactional
    public DishResponse createDish(DishRequest request) {
        log.debug("创建菜品: {}", request.getName());
        Dish dish = Dish.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .spicyLevel(request.getSpicyLevel() != null ? request.getSpicyLevel() : 0)
                .rating(request.getRating() != null ? request.getRating() : 0.0)
                .salesCount(0)
                .build();
        dish = dishRepository.save(dish);
        return toResponse(dish);
    }

    @Override
    @Transactional
    public DishResponse updateDish(Long id, DishRequest request) {
        log.debug("更新菜品 ID: {}", id);
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("菜品不存在: " + id));

        if (request.getName() != null) dish.setName(request.getName());
        if (request.getDescription() != null) dish.setDescription(request.getDescription());
        if (request.getPrice() != null) dish.setPrice(request.getPrice());
        if (request.getCategory() != null) dish.setCategory(request.getCategory());
        if (request.getImageUrl() != null) dish.setImageUrl(request.getImageUrl());
        if (request.getIsAvailable() != null) dish.setIsAvailable(request.getIsAvailable());
        if (request.getSpicyLevel() != null) dish.setSpicyLevel(request.getSpicyLevel());
        if (request.getRating() != null) dish.setRating(request.getRating());

        dish = dishRepository.save(dish);
        return toResponse(dish);
    }

    @Override
    @Transactional
    public void deleteDish(Long id) {
        log.debug("删除菜品 ID: {}", id);
        if (!dishRepository.existsById(id)) {
            throw new RuntimeException("菜品不存在: " + id);
        }
        dishRepository.deleteById(id);
    }

    @Override
    public List<DishResponse> searchDishes(String keyword) {
        log.debug("搜索菜品: {}", keyword);
        return dishRepository.findByNameContaining(keyword).stream()
                .filter(dish -> dish.getIsAvailable())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DishResponse> getTopSales() {
        log.debug("获取销量最高的菜品");
        return dishRepository.findTop10ByOrderBySalesCountDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DishResponse> getTopRated() {
        log.debug("获取评分最高的菜品");
        return dishRepository.findTop10ByOrderByRatingDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
                .spicyLevel(dish.getSpicyLevel())
                .rating(dish.getRating())
                .salesCount(dish.getSalesCount())
                .build();
    }
}