package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.AdmissionApplicationStatus;
import com.gmall.foundation.domain.model.AdmissionReviewTaskStatus;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoundationAdmissionService {

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final AdmissionIdempotencySupport admissionIdempotencySupport;
    private final AdmissionApprovalProvisioner admissionApprovalProvisioner;
    private final AdmissionWorkflowSupport admissionWorkflowSupport;
    private final AdmissionReviewTaskSupport admissionReviewTaskSupport;

    public FoundationAdmissionService(AdmissionApplicationRepository admissionApplicationRepository,
                                      AdmissionIdempotencySupport admissionIdempotencySupport, AdmissionApprovalProvisioner admissionApprovalProvisioner,
                                      AdmissionWorkflowSupport admissionWorkflowSupport, AdmissionReviewTaskSupport admissionReviewTaskSupport) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.admissionIdempotencySupport = admissionIdempotencySupport;
        this.admissionApprovalProvisioner = admissionApprovalProvisioner;
        this.admissionWorkflowSupport = admissionWorkflowSupport;
        this.admissionReviewTaskSupport = admissionReviewTaskSupport;
    }

    @Transactional
    public AdmissionApplicationEntity submit(AdmissionApplicationCommand command) {
        AdmissionApplicationEntity existing = admissionApplicationRepository.findById(command.applicationId())
                .orElse(null);
        if (existing != null) {
            return admissionIdempotencySupport.ensureSameSubmission(existing, command);
        }
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                command.applicationId(),
                command.organizationType(),
                AdmissionApplicationStatus.SUBMITTED,
                command.applicantName(),
                command.businessLicenseNo(),
                command.contactName(),
                command.contactMobile(),
                null,
                null,
                null,
                OffsetDateTime.now(),
                1L
        );
        return admissionApplicationRepository.save(application);
    }

    @Transactional
    public FoundationQueryModels.OrganizationView review(String applicationId, AdmissionReviewCommand command) {
        if (!"APPROVED".equals(command.decision())) {
            throw new IllegalStateException("当前审核接口仅支持通过后开通组织");
        }
        AdmissionApplicationEntity application = admissionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("入驻申请不存在: " + applicationId));
        String requestFingerprint = admissionIdempotencySupport.fingerprint(command.decision(), command.reviewComment(), command.reviewedBy(),
                command.organizationId(), command.defaultLocale(), command.supportedLocales(), command.admissionChannel(),
                command.industryCategory(), command.businessScope(), command.remark());
        FoundationQueryModels.OrganizationView retried = admissionIdempotencySupport.findRetriedApproval(applicationId, application, command, requestFingerprint);
        if (retried != null) {
            return retried;
        }
        String fromStatus = application.getApplicationStatus().name();
        OffsetDateTime operatedAt = OffsetDateTime.now();
        admissionWorkflowSupport.ensureUnderReview(application, "当前状态不允许审核通过");
        application.approve(command.reviewComment(), command.reviewedBy(), operatedAt);
        admissionApplicationRepository.save(application);
        admissionWorkflowSupport.appendReviewRecord(
                application,
                fromStatus,
                "APPROVE",
                command.reviewedBy(),
                command.reviewComment(),
                operatedAt
        );
        admissionReviewTaskSupport.completeTask(application, AdmissionReviewTaskStatus.APPROVED, command.reviewedBy(), command.reviewComment(), operatedAt);
        FoundationQueryModels.OrganizationView organizationView = admissionApprovalProvisioner.provision(applicationId, application, command);
        admissionIdempotencySupport.appendOperationLog(applicationId, "APPROVE", requestFingerprint, application.getAggregateVersion(), operatedAt);
        return organizationView;
    }
    @Transactional
    public AdmissionApplicationEntity startReview(String applicationId, FoundationOperationCommand command) {
        AdmissionApplicationEntity application = admissionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("入驻申请不存在: " + applicationId));
        String requestFingerprint = admissionIdempotencySupport.fingerprint(command.operatorId(), command.operationReason());
        AdmissionApplicationEntity retried = admissionIdempotencySupport.findRetriedOperation(
                applicationId,
                "START_REVIEW",
                requestFingerprint,
                application,
                AdmissionApplicationStatus.UNDER_REVIEW,
                command.operatorId(),
                command.operationReason()
        );
        if (retried != null) {
            return retried;
        }
        String fromStatus = application.getApplicationStatus().name();
        OffsetDateTime operatedAt = OffsetDateTime.now();
        application.startReview(command.operatorId(), command.operationReason(), operatedAt);
        AdmissionApplicationEntity saved = admissionApplicationRepository.save(application);
        admissionWorkflowSupport.appendReviewRecord(
                saved,
                fromStatus,
                "START_REVIEW",
                command.operatorId(),
                command.operationReason(),
                operatedAt
        );
        admissionReviewTaskSupport.createTask(saved, command.operatorId(), command.operatorId(), command.operationReason(), operatedAt);
        admissionIdempotencySupport.appendOperationLog(applicationId, "START_REVIEW", requestFingerprint, saved.getAggregateVersion(), operatedAt);
        return saved;
    }
    @Transactional
    public AdmissionApplicationEntity reject(String applicationId, FoundationOperationCommand command) {
        AdmissionApplicationEntity application = admissionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("入驻申请不存在: " + applicationId));
        String requestFingerprint = admissionIdempotencySupport.fingerprint(command.operatorId(), command.operationReason());
        AdmissionApplicationEntity retried = admissionIdempotencySupport.findRetriedOperation(
                applicationId,
                "REJECT",
                requestFingerprint,
                application,
                AdmissionApplicationStatus.REJECTED,
                command.operatorId(),
                command.operationReason()
        );
        if (retried != null) {
            return retried;
        }
        String fromStatus = application.getApplicationStatus().name();
        OffsetDateTime operatedAt = OffsetDateTime.now();
        admissionWorkflowSupport.ensureUnderReview(application, "当前状态不允许审核拒绝");
        admissionReviewTaskSupport.completeTask(application, AdmissionReviewTaskStatus.REJECTED, command.operatorId(), command.operationReason(), operatedAt);
        application.reject(command.operationReason(), command.operatorId(), operatedAt);
        AdmissionApplicationEntity saved = admissionApplicationRepository.save(application);
        admissionWorkflowSupport.appendReviewRecord(
                saved,
                fromStatus,
                "REJECT",
                command.operatorId(),
                command.operationReason(),
                operatedAt
        );
        admissionWorkflowSupport.publishRejectedEvent(saved, command.operatorId(), command.operationReason());
        admissionIdempotencySupport.appendOperationLog(applicationId, "REJECT", requestFingerprint, saved.getAggregateVersion(), operatedAt);
        return saved;
    }
    @Transactional
    public AdmissionApplicationEntity withdraw(String applicationId, FoundationOperationCommand command) {
        AdmissionApplicationEntity application = admissionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("入驻申请不存在: " + applicationId));
        String requestFingerprint = admissionIdempotencySupport.fingerprint(command.operatorId(), command.operationReason());
        AdmissionApplicationEntity retried = admissionIdempotencySupport.findRetriedOperation(
                applicationId,
                "WITHDRAW",
                requestFingerprint,
                application,
                AdmissionApplicationStatus.WITHDRAWN,
                command.operatorId(),
                command.operationReason()
        );
        if (retried != null) {
            return retried;
        }
        String fromStatus = application.getApplicationStatus().name();
        OffsetDateTime operatedAt = OffsetDateTime.now();
        application.withdraw(command.operatorId(), command.operationReason(), operatedAt);
        AdmissionApplicationEntity saved = admissionApplicationRepository.save(application);
        admissionWorkflowSupport.appendReviewRecord(
                saved,
                fromStatus,
                "WITHDRAW",
                command.operatorId(),
                command.operationReason(),
                operatedAt
        );
        admissionIdempotencySupport.appendOperationLog(applicationId, "WITHDRAW", requestFingerprint, saved.getAggregateVersion(), operatedAt);
        return saved;
    }
    @Transactional
    public AdmissionApplicationEntity resubmit(String applicationId, AdmissionResubmitCommand command) {
        if (!applicationId.equals(command.applicationId())) {
            throw new IllegalArgumentException("重提申请编号不匹配: " + applicationId);
        }
        AdmissionApplicationEntity application = admissionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("入驻申请不存在: " + applicationId));
        String requestFingerprint = admissionIdempotencySupport.fingerprint(command.organizationType(), command.applicantName(), command.businessLicenseNo(),
                command.contactName(), command.contactMobile(), command.operatorId(), command.operationReason());
        AdmissionApplicationEntity retried = admissionIdempotencySupport.findRetriedResubmit(applicationId, command, application, requestFingerprint);
        if (retried != null) {
            return retried;
        }
        String fromStatus = application.getApplicationStatus().name();
        OffsetDateTime operatedAt = OffsetDateTime.now();
        application.resubmit(command.applicantName(), command.businessLicenseNo(), command.contactName(), command.contactMobile(), operatedAt);
        AdmissionApplicationEntity saved = admissionApplicationRepository.save(application);
        admissionWorkflowSupport.appendReviewRecord(
                saved,
                fromStatus,
                "RESUBMIT",
                command.operatorId(),
                command.operationReason(),
                operatedAt
        );
        admissionIdempotencySupport.appendOperationLog(applicationId, "RESUBMIT", requestFingerprint, saved.getAggregateVersion(), operatedAt);
        return saved;
    }
    @Transactional
    public AdmissionApplicationEntity reassignReview(String applicationId, AdmissionReviewAssignmentCommand command) {
        AdmissionApplicationEntity application = admissionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("入驻申请不存在: " + applicationId));
        String requestFingerprint = admissionIdempotencySupport.fingerprint(command.operatorId(), command.reviewerId(), command.operationReason());
        AdmissionApplicationEntity retried = admissionIdempotencySupport.findRetriedReassign(applicationId, command, application, requestFingerprint);
        if (retried != null) {
            return retried;
        }
        String fromStatus = application.getApplicationStatus().name();
        OffsetDateTime operatedAt = OffsetDateTime.now();
        admissionWorkflowSupport.ensureUnderReview(application, "当前状态不允许转派审核");
        admissionReviewTaskSupport.reassignTask(application, command.reviewerId(), command.operatorId(), command.operationReason(), operatedAt);
        application.reassignReview(command.reviewerId(), command.operationReason(), operatedAt);
        AdmissionApplicationEntity saved = admissionApplicationRepository.save(application);
        admissionWorkflowSupport.appendReviewRecord(
                saved,
                fromStatus,
                "REASSIGN_REVIEW",
                command.operatorId(),
                "转派给审核人 " + command.reviewerId() + ": " + command.operationReason(),
                operatedAt
        );
        admissionIdempotencySupport.appendOperationLog(applicationId, "REASSIGN_REVIEW", requestFingerprint, saved.getAggregateVersion(), operatedAt);
        return saved;
    }
    @Transactional
    public AdmissionApplicationEntity timeoutReview(String applicationId, FoundationOperationCommand command) {
        AdmissionApplicationEntity application = admissionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("入驻申请不存在: " + applicationId));
        String requestFingerprint = admissionIdempotencySupport.fingerprint(command.operatorId(), command.operationReason());
        AdmissionApplicationEntity retried = admissionIdempotencySupport.findRetriedOperation(applicationId, "TIMEOUT_REVIEW", requestFingerprint, application,
                AdmissionApplicationStatus.SUBMITTED, command.operatorId(), command.operationReason());
        if (retried != null) {
            return retried;
        }
        OffsetDateTime operatedAt = OffsetDateTime.now();
        admissionWorkflowSupport.ensureTimeoutReviewable(application, operatedAt);
        String fromStatus = application.getApplicationStatus().name();
        admissionReviewTaskSupport.completeTask(application, AdmissionReviewTaskStatus.TIMED_OUT, command.operatorId(), command.operationReason(), operatedAt);
        application.timeoutReview(command.operatorId(), command.operationReason(), operatedAt);
        AdmissionApplicationEntity saved = admissionApplicationRepository.save(application);
        admissionWorkflowSupport.appendReviewRecord(
                saved,
                fromStatus,
                "TIMEOUT_REVIEW",
                command.operatorId(),
                command.operationReason(),
                operatedAt
        );
        admissionIdempotencySupport.appendOperationLog(applicationId, "TIMEOUT_REVIEW", requestFingerprint, saved.getAggregateVersion(), operatedAt);
        return saved;
    }
    @Transactional(readOnly = true)
    public FoundationQueryModels.AdmissionReviewTaskView getReviewTask(String applicationId) {
        return admissionReviewTaskSupport.getReviewTask(applicationId);
    }

    @Transactional(readOnly = true)
    public List<FoundationQueryModels.AdmissionReviewTaskQueueItemView> listReviewTasks(String taskStatus, String reviewerId) {
        return admissionReviewTaskSupport.listReviewTasks(taskStatus, reviewerId);
    }

    @Transactional(readOnly = true)
    public List<FoundationQueryModels.AdmissionPendingApplicationView> listPendingApplications(String organizationType) {
        List<AdmissionApplicationEntity> applications = organizationType == null || organizationType.isBlank()
                ? admissionApplicationRepository.findAllByApplicationStatusOrderBySubmittedAtAsc(AdmissionApplicationStatus.SUBMITTED)
                : admissionApplicationRepository.findAllByApplicationStatusAndOrganizationTypeOrderBySubmittedAtAsc(
                        AdmissionApplicationStatus.SUBMITTED,
                        organizationType
                );
        return applications.stream()
                .map(application -> new FoundationQueryModels.AdmissionPendingApplicationView(
                        application.getApplicationId(),
                        application.getOrganizationType(),
                        application.getApplicantName(),
                        application.getContactName(),
                        application.getContactMobile(),
                        application.getSubmittedAt(),
                        application.getApplicationStatus().name()
                ))
                .toList();
    }
}
