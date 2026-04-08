package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.OrganizationStatus;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organization_profile")
public class OrganizationEntity {

    @Id
    private String organizationId;

    @Column(nullable = false)
    private String organizationType;

    @Column(nullable = false)
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status;

    private String sourceApplicationId;

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

    protected OrganizationEntity() {
    }

    public OrganizationEntity(String organizationId, String organizationType, OrganizationStatus status,
                              String defaultLocale, String supportedLocales, String operatorId,
                              String operationReason, OffsetDateTime operationAt, Long aggregateVersion) {
        this(organizationId, organizationType, organizationId, status, null, defaultLocale, supportedLocales,
                operatorId, operationReason, operationAt, aggregateVersion);
    }

    public OrganizationEntity(String organizationId,
                              String organizationType,
                              String organizationName,
                              OrganizationStatus status,
                              String sourceApplicationId,
                              String defaultLocale,
                              String supportedLocales,
                              String operatorId,
                              String operationReason,
                              OffsetDateTime operationAt,
                              Long aggregateVersion) {
        this.organizationId = organizationId;
        this.organizationType = organizationType;
        this.organizationName = organizationName;
        this.status = status;
        this.sourceApplicationId = sourceApplicationId;
        this.defaultLocale = defaultLocale;
        this.supportedLocales = supportedLocales;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public String getSourceApplicationId() {
        return sourceApplicationId;
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

    public void changeStatus(OrganizationStatus status, String operatorId, String operationReason, OffsetDateTime operationAt) {
        this.status = status;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion + 1;
    }
}
