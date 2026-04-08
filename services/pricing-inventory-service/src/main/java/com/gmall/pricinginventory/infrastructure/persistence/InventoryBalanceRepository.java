package com.gmall.pricinginventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalanceEntity, String> {
}
