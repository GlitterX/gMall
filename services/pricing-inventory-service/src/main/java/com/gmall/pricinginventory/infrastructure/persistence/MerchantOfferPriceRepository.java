package com.gmall.pricinginventory.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MerchantOfferPriceRepository extends JpaRepository<MerchantOfferPriceEntity, String> {

    @Query("select p from MerchantOfferPriceEntity p where p.merchantOfferSkuId = ?1 and p.status = 'ACTIVE'")
    Optional<MerchantOfferPriceEntity> findActiveByMerchantOfferSkuId(String merchantOfferSkuId);
}
