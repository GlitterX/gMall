package com.gmall.pricinginventory.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SourceSupplyPriceRepository extends JpaRepository<SourceSupplyPriceEntity, String> {

    @Query("select p from SourceSupplyPriceEntity p where p.sourceSkuId = ?1 and p.status = 'ACTIVE'")
    Optional<SourceSupplyPriceEntity> findActiveBySourceSkuId(String sourceSkuId);
}
