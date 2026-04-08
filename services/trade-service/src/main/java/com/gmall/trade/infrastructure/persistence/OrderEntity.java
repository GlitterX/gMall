package com.gmall.trade.infrastructure.persistence;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trade_order")
public class OrderEntity {

    @Id
    private String orderId;

    @Column(nullable = false, unique = true)
    private String orderNo;

    @Column(nullable = false)
    private String businessIdempotencyKey;

    @Column(nullable = false)
    private String buyerId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String sellerType;

    @Column(nullable = false)
    private String sellerOfRecord;

    @Column(nullable = false)
    private String settlementBeneficiary;

    @Column(nullable = false)
    private String orderScene;

    @Column(nullable = false)
    private String storefrontId;

    @Column(nullable = false)
    private String currencyCode;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false)
    private long freightAmount;

    @Column(nullable = false)
    private long discountAmount;

    @Column(nullable = false)
    private long payableAmount;

    @Column(nullable = false, length = 4000)
    private String addressSnapshot;

    @Column(nullable = false)
    private String pricingSnapshotRef;

    @Column(nullable = false)
    private String inventoryReservationRef;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime submittedAt;

    protected OrderEntity() {
    }

    public OrderEntity(String orderId, String orderNo, String businessIdempotencyKey, String buyerId, String sellerId,
                       String sellerType, String sellerOfRecord, String settlementBeneficiary, String orderScene,
                       String storefrontId, String currencyCode, long totalAmount, long freightAmount,
                       long discountAmount, long payableAmount, String addressSnapshot, String pricingSnapshotRef,
                       String inventoryReservationRef, String status, OffsetDateTime submittedAt) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.businessIdempotencyKey = businessIdempotencyKey;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.sellerType = sellerType;
        this.sellerOfRecord = sellerOfRecord;
        this.settlementBeneficiary = settlementBeneficiary;
        this.orderScene = orderScene;
        this.storefrontId = storefrontId;
        this.currencyCode = currencyCode;
        this.totalAmount = totalAmount;
        this.freightAmount = freightAmount;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
        this.addressSnapshot = addressSnapshot;
        this.pricingSnapshotRef = pricingSnapshotRef;
        this.inventoryReservationRef = inventoryReservationRef;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSellerOfRecord() {
        return sellerOfRecord;
    }

    public String getSettlementBeneficiary() {
        return settlementBeneficiary;
    }

    public String getOrderScene() {
        return orderScene;
    }

    public String getStorefrontId() {
        return storefrontId;
    }

    public long getPayableAmount() {
        return payableAmount;
    }

    public String getPricingSnapshotRef() {
        return pricingSnapshotRef;
    }

    public String getInventoryReservationRef() {
        return inventoryReservationRef;
    }

    public String getStatus() {
        return status;
    }
}
