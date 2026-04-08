package com.gmall.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "catalog_projection")
public class CatalogProjectionEntity {

    @Id
    private String projectionId;

    @Column(nullable = false)
    private String presentationType;

    @Column(nullable = false)
    private String businessProductId;

    private String sourceProductId;

    private String merchantOfferProductId;

    @Column(nullable = false)
    private String productViewId;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false, length = 4000)
    private String projectionPayload;

    @Column(nullable = false)
    private String projectionStatus;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected CatalogProjectionEntity() {
    }

    public CatalogProjectionEntity(String projectionId,
                                   String presentationType,
                                   String businessProductId,
                                   String sourceProductId,
                                   String merchantOfferProductId,
                                   String productViewId,
                                   String locale,
                                   String projectionPayload,
                                   String projectionStatus,
                                   OffsetDateTime updatedAt) {
        this.projectionId = projectionId;
        this.presentationType = presentationType;
        this.businessProductId = businessProductId;
        this.sourceProductId = sourceProductId;
        this.merchantOfferProductId = merchantOfferProductId;
        this.productViewId = productViewId;
        this.locale = locale;
        this.projectionPayload = projectionPayload;
        this.projectionStatus = projectionStatus;
        this.updatedAt = updatedAt;
    }

    public void refresh(String projectionPayload, String projectionStatus, OffsetDateTime updatedAt) {
        this.projectionPayload = projectionPayload;
        this.projectionStatus = projectionStatus;
        this.updatedAt = updatedAt;
    }

    public String getPresentationType() {
        return presentationType;
    }

    public String getBusinessProductId() {
        return businessProductId;
    }

    public String getSourceProductId() {
        return sourceProductId;
    }

    public String getMerchantOfferProductId() {
        return merchantOfferProductId;
    }

    public String getProductViewId() {
        return productViewId;
    }

    public String getProjectionPayload() {
        return projectionPayload;
    }
}
