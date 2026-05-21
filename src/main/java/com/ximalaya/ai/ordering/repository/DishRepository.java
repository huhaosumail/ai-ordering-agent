
package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByIsAvailableTrue();

    List<Dish> findByCategory(String category);

    List<Dish> findByCategoryAndIsAvailableTrue(String category);

    List<Dish> findByNameContaining(String name);

    List<Dish> findTop10ByOrderBySalesCountDesc();

    List<Dish> findTop10ByOrderByRatingDesc();
}