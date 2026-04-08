package com.gmall.foundation.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplyRelationRepository extends JpaRepository<SupplyRelationEntity, String> {

    Optional<SupplyRelationEntity> findBySupplierOrganizationIdAndMerchantOrganizationId(String supplierOrganizationId,
                                                                                          String merchantOrganizationId);
}
