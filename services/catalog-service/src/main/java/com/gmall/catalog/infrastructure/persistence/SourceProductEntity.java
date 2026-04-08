package com.gmall.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "catalog_source_product")
public class SourceProductEntity {

    @Id
    private String sourceProductId;

    @Column(nullable = false)
    private String productViewId;

    @Column(nullable = false)
    private String ownerType;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private String sourceMode;

    @Column(nullable = false)
    private String categoryId;

    private String brandId;

    @Column(nullable = false, length = 4000)
    private String productContent;

    @Column(nullable = false)
    private String productStatus;

    @Column(nullable = false)
    private long contentVersion;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected SourceProductEntity() {
    }

    public SourceProductEntity(String sourceProductId,
                               String productViewId,
                               String ownerType,
                               String ownerId,
                               String sourceMode,
                               String categoryId,
                               String brandId,
                               String productContent,
                               String productStatus,
                               long contentVersion,
                               OffsetDateTime createdAt,
                               OffsetDateTime updatedAt) {
        this.sourceProductId = sourceProductId;
        this.productViewId = productViewId;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.sourceMode = sourceMode;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.productContent = productContent;
        this.productStatus = productStatus;
        this.contentVersion = contentVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getSourceProductId() {
        return sourceProductId;
    }

    public String getProductViewId() {
        return productViewId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public String getSourceMode() {
        return sourceMode;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getBrandId() {
        return brandId;
    }

    public String getProductContent() {
        return productContent;
    }

    public String getProductStatus() {
        return productStatus;
    }

    public long getContentVersion() {
        return contentVersion;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String productContent, String productStatus, OffsetDateTime updatedAt) {
        this.productContent = productContent;
        this.productStatus = productStatus;
        this.contentVersion += 1;
        this.updatedAt = updatedAt;
    }
}
