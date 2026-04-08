package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.AdmissionReviewTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admission_review_task")
public class AdmissionReviewTaskEntity {

    @Id
    private String taskId;

    @Column(nullable = false)
    private String applicationId;

    @Column(nullable = false)
    private Long taskSequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdmissionReviewTaskStatus taskStatus;

    @Column(nullable = false)
    private String reviewerId;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    @Column(nullable = false)
    private OffsetDateTime deadlineAt;

    @Column(nullable = false)
    private String lastOperatedBy;

    @Column(nullable = false)
    private String lastOperationReason;

    @Column(nullable = false)
    private OffsetDateTime lastOperatedAt;

    private OffsetDateTime closedAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    protected AdmissionReviewTaskEntity() {
    }

    public AdmissionReviewTaskEntity(String applicationId,
                                     Long taskSequence,
                                     AdmissionReviewTaskStatus taskStatus,
                                     String reviewerId,
                                     OffsetDateTime startedAt,
                                     OffsetDateTime deadlineAt,
                                     String lastOperatedBy,
                                     String lastOperationReason,
                                     OffsetDateTime lastOperatedAt,
                                     OffsetDateTime closedAt,
                                     Long aggregateVersion) {
        this.taskId = buildTaskId(applicationId, taskSequence);
        this.applicationId = applicationId;
        this.taskSequence = taskSequence;
        this.taskStatus = taskStatus;
        this.reviewerId = reviewerId;
        this.startedAt = startedAt;
        this.deadlineAt = deadlineAt;
        this.lastOperatedBy = lastOperatedBy;
        this.lastOperationReason = lastOperationReason;
        this.lastOperatedAt = lastOperatedAt;
        this.closedAt = closedAt;
        this.aggregateVersion = aggregateVersion;
    }

    public static String buildTaskId(String applicationId, Long taskSequence) {
        return applicationId + "::" + taskSequence;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public Long getTaskSequence() {
        return taskSequence;
    }

    public AdmissionReviewTaskStatus getTaskStatus() {
        return taskStatus;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public String getLastOperatedBy() {
        return lastOperatedBy;
    }

    public String getLastOperationReason() {
        return lastOperationReason;
    }

    public OffsetDateTime getLastOperatedAt() {
        return lastOperatedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public void reassign(String reviewerId,
                         String operatorId,
                         String operationReason,
                         OffsetDateTime startedAt,
                         OffsetDateTime deadlineAt) {
        ensureActive("当前审核任务不允许转派");
        this.reviewerId = reviewerId;
        this.startedAt = startedAt;
        this.deadlineAt = deadlineAt;
        this.lastOperatedBy = operatorId;
        this.lastOperationReason = operationReason;
        this.lastOperatedAt = startedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    public void close(AdmissionReviewTaskStatus taskStatus,
                      String operatorId,
                      String operationReason,
                      OffsetDateTime operatedAt) {
        ensureActive("当前审核任务不允许关闭");
        if (taskStatus == AdmissionReviewTaskStatus.ACTIVE) {
            throw new IllegalArgumentException("关闭审核任务时不能保持 ACTIVE");
        }
        this.taskStatus = taskStatus;
        this.lastOperatedBy = operatorId;
        this.lastOperationReason = operationReason;
        this.lastOperatedAt = operatedAt;
        this.closedAt = operatedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    private void ensureActive(String message) {
        if (taskStatus != AdmissionReviewTaskStatus.ACTIVE) {
            throw new IllegalStateException(message + ": " + taskStatus.name());
        }
    }
}
