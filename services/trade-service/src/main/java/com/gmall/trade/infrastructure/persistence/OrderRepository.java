package com.gmall.trade.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    List<OrderEntity> findByBusinessIdempotencyKeyOrderByOrderNoAsc(String businessIdempotencyKey);
}
