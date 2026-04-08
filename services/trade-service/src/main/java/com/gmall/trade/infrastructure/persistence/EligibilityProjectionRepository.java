package com.gmall.trade.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibilityProjectionRepository extends JpaRepository<EligibilityProjectionEntity, String> {

    Optional<EligibilityProjectionEntity> findBySellerIdAndStorefrontId(String sellerId, String storefrontId);

    List<EligibilityProjectionEntity> findBySellerId(String sellerId);

    List<EligibilityProjectionEntity> findByOrganizationId(String organizationId);
}
