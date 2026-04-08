package com.gmall.pricinginventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_ledger_entry")
public class InventoryLedgerEntryEntity {

    @Id
    private String ledgerId;

    @Column(nullable = false)
    private String sourceSkuId;

    @Column(nullable = false)
    private String businessKey;

    @Column(nullable = false)
    private String changeType;

    @Column(nullable = false)
    private long deltaQty;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected InventoryLedgerEntryEntity() {
    }

    public InventoryLedgerEntryEntity(String ledgerId,
                                      String sourceSkuId,
                                      String businessKey,
                                      String changeType,
                                      long deltaQty,
                                      OffsetDateTime createdAt) {
        this.ledgerId = ledgerId;
        this.sourceSkuId = sourceSkuId;
        this.businessKey = businessKey;
        this.changeType = changeType;
        this.deltaQty = deltaQty;
        this.createdAt = createdAt;
    }

    public String getSourceSkuId() {
        return sourceSkuId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getChangeType() {
        return changeType;
    }

    public long getDeltaQty() {
        return deltaQty;
    }
}
