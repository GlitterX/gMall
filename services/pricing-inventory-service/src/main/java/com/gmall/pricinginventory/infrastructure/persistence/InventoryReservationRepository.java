package com.gmall.pricinginventory.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, String> {

    Optional<InventoryReservationEntity> findByBusinessKey(String businessKey);
}
