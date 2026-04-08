package com.gmall.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "catalog_merchant_offer_product")
public class MerchantOfferProductEntity {

    @Id
    private String merchantOfferProductId;

    @Column(nullable = false)
    private String productViewId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String relationId;

    @Column(nullable = false)
    private String sourceProductId;

    @Column(nullable = false, length = 4000)
    private String offerContent;

    @Column(nullable = false)
    private String offerStatus;

    @Column(nullable = false)
    private String syncConfirmationStatus;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected MerchantOfferProductEntity() {
    }

    public MerchantOfferProductEntity(String merchantOfferProductId,
                                      String productViewId,
                                      String merchantId,
                                      String relationId,
                                      String sourceProductId,
                                      String offerContent,
                                      String offerStatus,
                                      String syncConfirmationStatus,
                                      OffsetDateTime createdAt,
                                      OffsetDateTime updatedAt) {
        this.merchantOfferProductId = merchantOfferProductId;
        this.productViewId = productViewId;
        this.merchantId = merchantId;
        this.relationId = relationId;
        this.sourceProductId = sourceProductId;
        this.offerContent = offerContent;
        this.offerStatus = offerStatus;
        this.syncConfirmationStatus = syncConfirmationStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getMerchantOfferProductId() {
        return merchantOfferProductId;
    }

    public String getProductViewId() {
        return productViewId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getRelationId() {
        return relationId;
    }

    public String getSourceProductId() {
        return sourceProductId;
    }

    public String getOfferContent() {
        return offerContent;
    }

    public String getOfferStatus() {
        return offerStatus;
    }

    public String getSyncConfirmationStatus() {
        return syncConfirmationStatus;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateOfferContent(String offerContent, String offerStatus, OffsetDateTime updatedAt) {
        this.offerContent = offerContent;
        this.offerStatus = offerStatus;
        this.syncConfirmationStatus = "SYNCED";
        this.updatedAt = updatedAt;
    }

    public void markInvalidPendingConfirm(OffsetDateTime updatedAt) {
        this.offerStatus = "INVALID_PENDING_CONFIRM";
        this.syncConfirmationStatus = "PENDING_CONFIRM";
        this.updatedAt = updatedAt;
    }
}
