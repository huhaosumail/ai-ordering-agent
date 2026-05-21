package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.Dish;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DishRepository extends ReactiveCrudRepository<Dish, Long> {

    Flux<Dish> findByIsAvailableTrue();

    Flux<Dish> findByCategory(String category);

    @Query("SELECT * FROM dish WHERE is_available = true ORDER BY sales_count DESC LIMIT :limit")
    Flux<Dish> findTopSales(int limit);

    @Query("SELECT * FROM dish WHERE is_available = true AND rating_count > 0 ORDER BY rating DESC LIMIT :limit")
    Flux<Dish> findTopRated(int limit);

    @Query("SELECT * FROM dish WHERE is_available = true AND (name LIKE :keyword OR description LIKE :keyword)")
    Flux<Dish> searchByKeyword(String keyword);

    @Query("SELECT * FROM dish WHERE name LIKE :name")
    Flux<Dish> findByNameLike(String name);
}