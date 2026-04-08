package com.gmall.trade.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubOrderRepository extends JpaRepository<SubOrderEntity, String> {
}
