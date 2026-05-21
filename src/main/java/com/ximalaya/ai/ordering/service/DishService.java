package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.dto.request.DishRequest;
import com.ximalaya.ai.ordering.dto.response.DishResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DishService {

    Flux<DishResponse> getAvailableDishes();

    Mono<DishResponse> getDishById(Long id);

    Flux<DishResponse> getDishesByCategory(String category);

    Flux<DishResponse> searchDishes(String keyword);

    Flux<DishResponse> getTopSales();

    Flux<DishResponse> getTopRated();

    Mono<DishResponse> createDish(DishRequest request);

    Mono<DishResponse> updateDish(Long id, DishRequest request);

    Mono<Void> deleteDish(Long id);
}