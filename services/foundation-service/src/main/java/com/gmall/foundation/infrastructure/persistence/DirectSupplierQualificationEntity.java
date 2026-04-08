package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.DirectQualificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "direct_supplier_qualification")
public class DirectSupplierQualificationEntity {

    @Id
    private String qualificationId;

    @Column(nullable = false, unique = true)
    private String organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectQualificationStatus status;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false)
    private String operationReason;

    @Column(nullable = false)
    private OffsetDateTime operationAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    protected DirectSupplierQualificationEntity() {
    }

    public DirectSupplierQualificationEntity(String qualificationId,
                                             String organizationId,
                                             DirectQualificationStatus status,
                                             String operatorId,
                                             String operationReason,
                                             OffsetDateTime operationAt,
                                             Long aggregateVersion) {
        this.qualificationId = qualificationId;
        this.organizationId = organizationId;
        this.status = status;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion;
    }

    public String getQualificationId() {
        return qualificationId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public DirectQualificationStatus getStatus() {
        return status;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public void changeStatus(DirectQualificationStatus status,
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
