package com.gmall.foundation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admission_operation_log")
public class AdmissionOperationLogEntity {

    @Id
    private String operationId;

    @Column(nullable = false)
    private String applicationId;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String requestFingerprint;

    @Column(nullable = false)
    private Long aggregateVersion;

    @Column(nullable = false)
    private OffsetDateTime operatedAt;

    protected AdmissionOperationLogEntity() {
    }

    public AdmissionOperationLogEntity(String applicationId,
                                       String actionType,
                                       String requestFingerprint,
                                       Long aggregateVersion,
                                       OffsetDateTime operatedAt) {
        this.operationId = buildOperationId(applicationId, actionType, requestFingerprint);
        this.applicationId = applicationId;
        this.actionType = actionType;
        this.requestFingerprint = requestFingerprint;
        this.aggregateVersion = aggregateVersion;
        this.operatedAt = operatedAt;
    }

    public static String buildOperationId(String applicationId, String actionType, String requestFingerprint) {
        return applicationId + "::" + actionType + "::" + requestFingerprint;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public OffsetDateTime getOperatedAt() {
        return operatedAt;
    }
}
