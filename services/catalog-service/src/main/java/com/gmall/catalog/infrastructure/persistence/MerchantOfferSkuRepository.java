package com.gmall.catalog.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantOfferSkuRepository extends JpaRepository<MerchantOfferSkuEntity, String> {

    List<MerchantOfferSkuEntity> findByMerchantOfferProductId(String merchantOfferProductId);
}
