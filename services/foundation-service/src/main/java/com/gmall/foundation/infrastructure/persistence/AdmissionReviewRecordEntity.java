package com.gmall.foundation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admission_review_record")
public class AdmissionReviewRecordEntity {

    @Id
    private String recordId;

    @Column(nullable = false)
    private String applicationId;

    @Column(nullable = false)
    private String fromStatus;

    @Column(nullable = false)
    private String toStatus;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false)
    private String operationReason;

    @Column(nullable = false)
    private OffsetDateTime operatedAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    protected AdmissionReviewRecordEntity() {
    }

    public AdmissionReviewRecordEntity(String applicationId,
                                       String fromStatus,
                                       String toStatus,
                                       String actionType,
                                       String operatorId,
                                       String operationReason,
                                       OffsetDateTime operatedAt,
                                       Long aggregateVersion) {
        this.recordId = buildRecordId(applicationId, aggregateVersion);
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actionType = actionType;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operatedAt = operatedAt;
        this.aggregateVersion = aggregateVersion;
    }

    public static String buildRecordId(String applicationId, Long aggregateVersion) {
        return applicationId + "::" + aggregateVersion;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public String getActionType() {
        return actionType;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getOperationReason() {
        return operationReason;
    }

    public OffsetDateTime getOperatedAt() {
        return operatedAt;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }
}
