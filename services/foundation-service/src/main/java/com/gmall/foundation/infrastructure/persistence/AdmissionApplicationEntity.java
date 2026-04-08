package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.AdmissionApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admission_application")
public class AdmissionApplicationEntity {

    @Id
    private String applicationId;

    @Column(nullable = false)
    private String organizationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdmissionApplicationStatus applicationStatus;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false)
    private String businessLicenseNo;

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String contactMobile;

    private String reviewComment;

    private String reviewedBy;

    private OffsetDateTime reviewedAt;

    @Column(nullable = false)
    private OffsetDateTime submittedAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    protected AdmissionApplicationEntity() {
    }

    public AdmissionApplicationEntity(String applicationId,
                                      String organizationType,
                                      AdmissionApplicationStatus applicationStatus,
                                      String applicantName,
                                      String businessLicenseNo,
                                      String contactName,
                                      String contactMobile,
                                      String reviewComment,
                                      String reviewedBy,
                                      OffsetDateTime reviewedAt,
                                      OffsetDateTime submittedAt,
                                      Long aggregateVersion) {
        this.applicationId = applicationId;
        this.organizationType = organizationType;
        this.applicationStatus = applicationStatus;
        this.applicantName = applicantName;
        this.businessLicenseNo = businessLicenseNo;
        this.contactName = contactName;
        this.contactMobile = contactMobile;
        this.reviewComment = reviewComment;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.submittedAt = submittedAt;
        this.aggregateVersion = aggregateVersion;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public AdmissionApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getBusinessLicenseNo() {
        return businessLicenseNo;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactMobile() {
        return contactMobile;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public void approve(String reviewComment, String reviewedBy, OffsetDateTime reviewedAt) {
        ensureStatus(AdmissionApplicationStatus.UNDER_REVIEW, "当前状态不允许审核通过");
        this.applicationStatus = AdmissionApplicationStatus.APPROVED;
        this.reviewComment = reviewComment;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    public void reject(String reviewComment, String reviewedBy, OffsetDateTime reviewedAt) {
        ensureStatus(AdmissionApplicationStatus.UNDER_REVIEW, "当前状态不允许审核拒绝");
        this.applicationStatus = AdmissionApplicationStatus.REJECTED;
        this.reviewComment = reviewComment;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    public void startReview(String reviewedBy, String reviewComment, OffsetDateTime reviewedAt) {
        ensureStatus(AdmissionApplicationStatus.SUBMITTED, "当前状态不允许开始审核");
        this.applicationStatus = AdmissionApplicationStatus.UNDER_REVIEW;
        this.reviewComment = reviewComment;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    public void withdraw(String operatorId, String operationReason, OffsetDateTime operatedAt) {
        ensureStatus(AdmissionApplicationStatus.SUBMITTED, "当前状态不允许撤回");
        this.applicationStatus = AdmissionApplicationStatus.WITHDRAWN;
        this.reviewComment = operationReason;
        this.reviewedBy = operatorId;
        this.reviewedAt = operatedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    public void resubmit(String applicantName,
                         String businessLicenseNo,
                         String contactName,
                         String contactMobile,
                         OffsetDateTime submittedAt) {
        ensureStatus(AdmissionApplicationStatus.REJECTED, "当前状态不允许重提");
        this.applicationStatus = AdmissionApplicationStatus.SUBMITTED;
        this.applicantName = applicantName;
        this.businessLicenseNo = businessLicenseNo;
        this.contactName = contactName;
        this.contactMobile = contactMobile;
        this.reviewComment = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.submittedAt = submittedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    public void reassignReview(String reviewerId, String operationReason, OffsetDateTime operatedAt) {
        ensureStatus(AdmissionApplicationStatus.UNDER_REVIEW, "当前状态不允许转派审核");
        this.reviewedBy = reviewerId;
        this.reviewComment = operationReason;
        this.reviewedAt = operatedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    public void timeoutReview(String operatorId, String operationReason, OffsetDateTime operatedAt) {
        ensureStatus(AdmissionApplicationStatus.UNDER_REVIEW, "当前状态不允许审核超时退回");
        this.applicationStatus = AdmissionApplicationStatus.SUBMITTED;
        this.reviewComment = operationReason;
        this.reviewedBy = operatorId;
        this.reviewedAt = operatedAt;
        this.aggregateVersion = aggregateVersion + 1;
    }

    public void review(String decision, String reviewComment, String reviewedBy, OffsetDateTime reviewedAt) {
        if ("APPROVED".equals(decision)) {
            approve(reviewComment, reviewedBy, reviewedAt);
            return;
        }
        if ("REJECTED".equals(decision)) {
            reject(reviewComment, reviewedBy, reviewedAt);
            return;
        }
        throw new IllegalArgumentException("不支持的审核决策: " + decision);
    }

    private void ensureStatus(AdmissionApplicationStatus expectedStatus, String message) {
        if (applicationStatus != expectedStatus) {
            throw new IllegalStateException(message + ": " + applicationStatus.name());
        }
    }
}
