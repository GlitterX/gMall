package com.gmall.decoration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "decoration_publish_record")
public class DecorationPublishRecordEntity {

    @Id
    private String publishId;

    @Column(nullable = false)
    private String pageId;

    private String draftId;

    private String reviewId;

    @Column(nullable = false)
    private String operationType;

    @Column(nullable = false)
    private String operationStatus;

    @Column(nullable = false)
    private String operationRequestId;

    @Column(nullable = false)
    private String targetTerminalType;

    private String fromSnapshotId;

    private String resultSnapshotId;

    private String rollbackTargetSnapshotId;

    @Column(nullable = false)
    private String operatorId;

    private String operationComment;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime operatedAt;

    protected DecorationPublishRecordEntity() {
    }

    public DecorationPublishRecordEntity(String publishId,
                                         String pageId,
                                         String draftId,
                                         String reviewId,
                                         String operationType,
                                         String operationStatus,
                                         String operationRequestId,
                                         String targetTerminalType,
                                         String fromSnapshotId,
                                         String resultSnapshotId,
                                         String rollbackTargetSnapshotId,
                                         String operatorId,
                                         String operationComment,
                                         OffsetDateTime createdAt,
                                         OffsetDateTime operatedAt) {
        this.publishId = publishId;
        this.pageId = pageId;
        this.draftId = draftId;
        this.reviewId = reviewId;
        this.operationType = operationType;
        this.operationStatus = operationStatus;
        this.operationRequestId = operationRequestId;
        this.targetTerminalType = targetTerminalType;
        this.fromSnapshotId = fromSnapshotId;
        this.resultSnapshotId = resultSnapshotId;
        this.rollbackTargetSnapshotId = rollbackTargetSnapshotId;
        this.operatorId = operatorId;
        this.operationComment = operationComment;
        this.createdAt = createdAt;
        this.operatedAt = operatedAt;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getOperationStatus() {
        return operationStatus;
    }

    public String getOperationRequestId() {
        return operationRequestId;
    }

    public String getResultSnapshotId() {
        return resultSnapshotId;
    }
}
