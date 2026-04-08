package com.gmall.foundation.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectSupplierQualificationRepository extends JpaRepository<DirectSupplierQualificationEntity, String> {

    Optional<DirectSupplierQualificationEntity> findByOrganizationId(String organizationId);
}
