
package com.ximalaya.ai.ordering.repository;

import com.ximalaya.ai.ordering.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(String status);

    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}