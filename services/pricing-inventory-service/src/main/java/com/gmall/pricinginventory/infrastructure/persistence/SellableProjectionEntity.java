package com.gmall.pricinginventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pricing_inventory_sellable_projection")
public class SellableProjectionEntity {

    @Id
    private String sellableId;

    @Column(nullable = false)
    private String businessSkuType;

    @Column(nullable = false)
    private String businessSkuId;

    @Column(nullable = false)
    private String sourceSkuId;

    @Column(nullable = false)
    private String sellableState;

    @Column(nullable = false)
    private String reasonCode;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected SellableProjectionEntity() {
    }

    public SellableProjectionEntity(String sellableId,
                                    String businessSkuType,
                                    String businessSkuId,
                                    String sourceSkuId,
                                    String sellableState,
                                    String reasonCode,
                                    OffsetDateTime updatedAt) {
        this.sellableId = sellableId;
        this.businessSkuType = businessSkuType;
        this.businessSkuId = businessSkuId;
        this.sourceSkuId = sourceSkuId;
        this.sellableState = sellableState;
        this.reasonCode = reasonCode;
        this.updatedAt = updatedAt;
    }

    public String getBusinessSkuType() {
        return businessSkuType;
    }

    public String getBusinessSkuId() {
        return businessSkuId;
    }

    public String getSourceSkuId() {
        return sourceSkuId;
    }

    public String getSellableState() {
        return sellableState;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
