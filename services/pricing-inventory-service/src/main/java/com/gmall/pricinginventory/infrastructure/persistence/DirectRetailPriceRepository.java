package com.gmall.pricinginventory.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DirectRetailPriceRepository extends JpaRepository<DirectRetailPriceEntity, String> {

    @Query("select p from DirectRetailPriceEntity p where p.sourceSkuId = ?1 and p.status = 'ACTIVE'")
    Optional<DirectRetailPriceEntity> findActiveBySourceSkuId(String sourceSkuId);
}
