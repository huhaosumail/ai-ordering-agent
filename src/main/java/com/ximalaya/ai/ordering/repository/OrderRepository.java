package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Mono<Order> findByOrderNo(String orderNo);

    Flux<Order> findByUserId(Long userId);

    Flux<Order> findByStatus(String status);
}