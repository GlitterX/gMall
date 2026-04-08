package com.gmall.pricinginventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "source_supply_price")
public class SourceSupplyPriceEntity {

    @Id
    private String priceId;

    @Column(nullable = false)
    private String sourceSkuId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private String currencyCode;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime effectiveAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected SourceSupplyPriceEntity() {
    }

    public SourceSupplyPriceEntity(String priceId,
                                   String sourceSkuId,
                                   long amount,
                                   String currencyCode,
                                   String status,
                                   OffsetDateTime effectiveAt,
                                   OffsetDateTime updatedAt) {
        this.priceId = priceId;
        this.sourceSkuId = sourceSkuId;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.status = status;
        this.effectiveAt = effectiveAt;
        this.updatedAt = updatedAt;
    }

    public long getAmount() {
        return amount;
    }

    public String getSourceSkuId() {
        return sourceSkuId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getEffectiveAt() {
        return effectiveAt;
    }
}
