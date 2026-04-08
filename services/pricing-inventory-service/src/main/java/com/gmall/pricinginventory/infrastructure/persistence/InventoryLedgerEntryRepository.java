package com.gmall.pricinginventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLedgerEntryRepository extends JpaRepository<InventoryLedgerEntryEntity, String> {
}
