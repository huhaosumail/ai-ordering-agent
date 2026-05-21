package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.Category;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CategoryRepository extends ReactiveCrudRepository<Category, Long> {

    Flux<Category> findAllByOrderBySortOrderAsc();

    Flux<Category> findByName(String name);
}