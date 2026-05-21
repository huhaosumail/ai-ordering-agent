
package com.ximalaya.ai.ordering.service;

import com.ximalaya.ai.ordering.dto.request.DishRequest;
import com.ximalaya.ai.ordering.dto.response.DishResponse;

import java.util.List;

public interface DishService {

    List<DishResponse> getAllDishes();

    List<DishResponse> getAvailableDishes();

    List<DishResponse> getDishesByCategory(String category);

    DishResponse getDishById(Long id);

    DishResponse createDish(DishRequest request);

    DishResponse updateDish(Long id, DishRequest request);

    void deleteDish(Long id);

    List<DishResponse> searchDishes(String keyword);

    List<DishResponse> getTopSales();

    List<DishResponse> getTopRated();
}