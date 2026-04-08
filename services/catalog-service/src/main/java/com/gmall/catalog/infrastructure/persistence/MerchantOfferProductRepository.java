package com.gmall.catalog.infrastructure.persistence;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantOfferProductRepository extends JpaRepository<MerchantOfferProductEntity, String> {

    Optional<MerchantOfferProductEntity> findByMerchantIdAndRelationIdAndSourceProductId(String merchantId,
                                                                                          String relationId,
                                                                                          String sourceProductId);

    java.util.List<MerchantOfferProductEntity> findBySourceProductId(String sourceProductId);

    List<MerchantOfferProductEntity> findByMerchantId(String merchantId);

    List<MerchantOfferProductEntity> findByMerchantIdOrderByUpdatedAtDesc(String merchantId);
}
