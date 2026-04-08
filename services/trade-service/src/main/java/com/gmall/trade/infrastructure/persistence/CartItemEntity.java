package com.gmall.trade.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trade_cart_item")
public class CartItemEntity {

    @Id
    private String cartItemId;

    @Column(nullable = false)
    private String buyerId;

    @Column(nullable = false)
    private String businessSkuType;

    @Column(name = "sku_id", nullable = false)
    private String businessSkuId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String storefrontId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean selected;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected CartItemEntity() {
    }

    public CartItemEntity(String cartItemId,
                          String buyerId,
                          String businessSkuType,
                          String businessSkuId,
                          String sellerId,
                          String storefrontId,
                          int quantity,
                          boolean selected,
                          OffsetDateTime createdAt,
                          OffsetDateTime updatedAt) {
        this.cartItemId = cartItemId;
        this.buyerId = buyerId;
        this.businessSkuType = businessSkuType;
        this.businessSkuId = businessSkuId;
        this.sellerId = sellerId;
        this.storefrontId = storefrontId;
        this.quantity = quantity;
        this.selected = selected;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void mergeQuantity(int delta, OffsetDateTime updatedAt) {
        this.quantity += delta;
        this.selected = true;
        this.updatedAt = updatedAt;
    }

    public void update(int quantity, boolean selected, OffsetDateTime updatedAt) {
        this.quantity = quantity;
        this.selected = selected;
        this.updatedAt = updatedAt;
    }

    public String getCartItemId() {
        return cartItemId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getBusinessSkuType() {
        return businessSkuType;
    }

    public String getBusinessSkuId() {
        return businessSkuId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getStorefrontId() {
        return storefrontId;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isSelected() {
        return selected;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
