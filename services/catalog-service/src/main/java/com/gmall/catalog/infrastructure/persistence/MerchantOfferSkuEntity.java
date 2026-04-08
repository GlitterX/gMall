package com.gmall.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_merchant_offer_sku")
public class MerchantOfferSkuEntity {

    @Id
    private String merchantOfferSkuId;

    @Column(nullable = false)
    private String merchantOfferProductId;

    @Column(nullable = false)
    private String sourceSkuId;

    @Column(nullable = false, length = 4000)
    private String offerSkuName;

    @Column(nullable = false)
    private String offerSkuStatus;

    @Column(nullable = false)
    private long mappingVersion;

    protected MerchantOfferSkuEntity() {
    }

    public MerchantOfferSkuEntity(String merchantOfferSkuId,
                                  String merchantOfferProductId,
                                  String sourceSkuId,
                                  String offerSkuName,
                                  String offerSkuStatus,
                                  long mappingVersion) {
        this.merchantOfferSkuId = merchantOfferSkuId;
        this.merchantOfferProductId = merchantOfferProductId;
        this.sourceSkuId = sourceSkuId;
        this.offerSkuName = offerSkuName;
        this.offerSkuStatus = offerSkuStatus;
        this.mappingVersion = mappingVersion;
    }

    public String getSourceSkuId() {
        return sourceSkuId;
    }

    public String getMerchantOfferSkuId() {
        return merchantOfferSkuId;
    }

    public String getMerchantOfferProductId() {
        return merchantOfferProductId;
    }

    public String getOfferSkuStatus() {
        return offerSkuStatus;
    }

    public long getMappingVersion() {
        return mappingVersion;
    }
}
