package com.gmall.pricinginventory.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingSnapshotRepository extends JpaRepository<PricingSnapshotEntity, String> {

    Optional<PricingSnapshotEntity> findByBusinessKey(String businessKey);
}
