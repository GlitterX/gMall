package com.gmall.pricinginventory.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellableProjectionRepository extends JpaRepository<SellableProjectionEntity, String> {

    Optional<SellableProjectionEntity> findByBusinessSkuTypeAndBusinessSkuId(String businessSkuType, String businessSkuId);
}
