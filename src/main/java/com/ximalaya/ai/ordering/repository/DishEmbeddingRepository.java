package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.DishEmbedding;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DishEmbeddingRepository extends ReactiveCrudRepository<DishEmbedding, Long> {
}
