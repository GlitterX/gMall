package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.StorefrontStatus;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "storefront_profile")
public class StorefrontEntity {

    @Id
    private String storefrontId;

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String storefrontType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorefrontStatus status;

    @Column(nullable = false)
    private String defaultLocale;

    @Column(nullable = false)
    private String supportedLocales;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false)
    private String operationReason;

    @Column(nullable = false)
    private OffsetDateTime operationAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    protected StorefrontEntity() {
    }

    public StorefrontEntity(String storefrontId, String organizationId, String sellerId, String storefrontType,
                            StorefrontStatus status, String defaultLocale, String supportedLocales,
                            String operatorId, String operationReason, OffsetDateTime operationAt,
                            Long aggregateVersion) {
        this.storefrontId = storefrontId;
        this.organizationId = organizationId;
        this.sellerId = sellerId;
        this.storefrontType = storefrontType;
        this.status = status;
        this.defaultLocale = defaultLocale;
        this.supportedLocales = supportedLocales;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion;
    }

    public String getStorefrontId() {
        return storefrontId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getStorefrontType() {
        return storefrontType;
    }

    public StorefrontStatus getStatus() {
        return status;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public String getSupportedLocales() {
        return supportedLocales;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public void changeStatus(StorefrontStatus status, String operatorId, String operationReason, OffsetDateTime operationAt) {
        this.status = status;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion + 1;
    }
}
