package com.gmall.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_source_sku")
public class SourceSkuEntity {

    @Id
    private String sourceSkuId;

    @Column(nullable = false)
    private String sourceProductId;

    @Column(nullable = false)
    private String specSignature;

    @Column(nullable = false, length = 4000)
    private String specPayload;

    @Column(nullable = false)
    private String skuStatus;

    @Column(nullable = false)
    private long version;

    protected SourceSkuEntity() {
    }

    public SourceSkuEntity(String sourceSkuId,
                           String sourceProductId,
                           String specSignature,
                           String specPayload,
                           String skuStatus,
                           long version) {
        this.sourceSkuId = sourceSkuId;
        this.sourceProductId = sourceProductId;
        this.specSignature = specSignature;
        this.specPayload = specPayload;
        this.skuStatus = skuStatus;
        this.version = version;
    }

    public String getSourceSkuId() {
        return sourceSkuId;
    }

    public String getSourceProductId() {
        return sourceProductId;
    }

    public String getSkuStatus() {
        return skuStatus;
    }

    public long getVersion() {
        return version;
    }
}
