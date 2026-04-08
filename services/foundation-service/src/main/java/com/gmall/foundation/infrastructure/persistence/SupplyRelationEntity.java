package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.SupplyRelationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "supply_relation")
public class SupplyRelationEntity {

    @Id
    private String relationId;

    @Column(nullable = false)
    private String supplierOrganizationId;

    @Column(nullable = false)
    private String merchantOrganizationId;

    @Column(nullable = false)
    private String authorizedCatalogScope;

    @Column(nullable = false)
    private String supplyPriceRule;

    @Column(nullable = false)
    private String settlementRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplyRelationStatus status;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false)
    private String operationReason;

    @Column(nullable = false)
    private OffsetDateTime operationAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    protected SupplyRelationEntity() {
    }

    public SupplyRelationEntity(String relationId,
                                String supplierOrganizationId,
                                String merchantOrganizationId,
                                String authorizedCatalogScope,
                                String supplyPriceRule,
                                String settlementRule,
                                SupplyRelationStatus status,
                                String operatorId,
                                String operationReason,
                                OffsetDateTime operationAt,
                                Long aggregateVersion) {
        this.relationId = relationId;
        this.supplierOrganizationId = supplierOrganizationId;
        this.merchantOrganizationId = merchantOrganizationId;
        this.authorizedCatalogScope = authorizedCatalogScope;
        this.supplyPriceRule = supplyPriceRule;
        this.settlementRule = settlementRule;
        this.status = status;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion;
    }

    public String getRelationId() {
        return relationId;
    }

    public String getSupplierOrganizationId() {
        return supplierOrganizationId;
    }

    public String getMerchantOrganizationId() {
        return merchantOrganizationId;
    }

    public SupplyRelationStatus getStatus() {
        return status;
    }

    public String getAuthorizedCatalogScope() {
        return authorizedCatalogScope;
    }

    public String getSupplyPriceRule() {
        return supplyPriceRule;
    }

    public String getSettlementRule() {
        return settlementRule;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public void changeStatus(SupplyRelationStatus status,
                             String operatorId,
                             String operationReason,
                             OffsetDateTime operationAt) {
        this.status = status;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion + 1;
    }
}
