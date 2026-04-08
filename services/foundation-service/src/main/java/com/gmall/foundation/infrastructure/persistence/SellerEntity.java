package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.SellerStatus;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "seller_profile")
public class SellerEntity {

    @Id
    private String sellerId;

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false)
    private String sellerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellerStatus status;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false)
    private String operationReason;

    @Column(nullable = false)
    private OffsetDateTime operationAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    protected SellerEntity() {
    }

    public SellerEntity(String sellerId, String organizationId, String sellerType, SellerStatus status,
                        String operatorId, String operationReason, OffsetDateTime operationAt,
                        Long aggregateVersion) {
        this.sellerId = sellerId;
        this.organizationId = organizationId;
        this.sellerType = sellerType;
        this.status = status;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getSellerType() {
        return sellerType;
    }

    public SellerStatus getStatus() {
        return status;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public void changeStatus(SellerStatus status, String operatorId, String operationReason, OffsetDateTime operationAt) {
        this.status = status;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion + 1;
    }
}
