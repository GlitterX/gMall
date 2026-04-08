package com.gmall.pricinginventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pricing_snapshot")
public class PricingSnapshotEntity {

    @Id
    private String pricingSnapshotRef;

    @Column(nullable = false, unique = true)
    private String businessKey;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false, length = 4000)
    private String snapshotPayload;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected PricingSnapshotEntity() {
    }

    public PricingSnapshotEntity(String pricingSnapshotRef,
                                 String businessKey,
                                 long totalAmount,
                                 String snapshotPayload,
                                 OffsetDateTime createdAt) {
        this.pricingSnapshotRef = pricingSnapshotRef;
        this.businessKey = businessKey;
        this.totalAmount = totalAmount;
        this.snapshotPayload = snapshotPayload;
        this.createdAt = createdAt;
    }

    public String getPricingSnapshotRef() {
        return pricingSnapshotRef;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public String getSnapshotPayload() {
        return snapshotPayload;
    }
}
