package com.gmall.pricinginventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_balance")
public class InventoryBalanceEntity {

    @Id
    private String sourceSkuId;

    @Column(nullable = false)
    private long availableQty;

    @Column(nullable = false)
    private long reservedQty;

    @Column(nullable = false)
    private long soldQty;

    @Column(nullable = false)
    private long version;

    protected InventoryBalanceEntity() {
    }

    public InventoryBalanceEntity(String sourceSkuId,
                                  long availableQty,
                                  long reservedQty,
                                  long soldQty,
                                  long version) {
        this.sourceSkuId = sourceSkuId;
        this.availableQty = availableQty;
        this.reservedQty = reservedQty;
        this.soldQty = soldQty;
        this.version = version;
    }

    public void reserve(long quantity) {
        if (availableQty < quantity) {
            throw new IllegalStateException("库存不足");
        }
        availableQty -= quantity;
        reservedQty += quantity;
        version += 1;
    }

    public void release(long quantity) {
        if (reservedQty < quantity) {
            throw new IllegalStateException("预占库存不足");
        }
        availableQty += quantity;
        reservedQty -= quantity;
        version += 1;
    }

    public void confirm(long quantity) {
        if (reservedQty < quantity) {
            throw new IllegalStateException("预占库存不足");
        }
        reservedQty -= quantity;
        soldQty += quantity;
        version += 1;
    }

    public void adjust(long deltaQuantity) {
        if (availableQty + deltaQuantity < 0) {
            throw new IllegalStateException("库存不足");
        }
        availableQty += deltaQuantity;
        version += 1;
    }

    public String getSourceSkuId() {
        return sourceSkuId;
    }

    public long getAvailableQty() {
        return availableQty;
    }

    public long getReservedQty() {
        return reservedQty;
    }

    public long getSoldQty() {
        return soldQty;
    }
}
