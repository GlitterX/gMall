package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.AdmissionApplicationStatus;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationRepository;
import com.gmall.foundation.infrastructure.persistence.AdmissionOperationLogEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionOperationLogRepository;
import com.gmall.foundation.infrastructure.persistence.OrganizationEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AdmissionIdempotencySupport {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final AdmissionOperationLogRepository admissionOperationLogRepository;
    private final OrganizationRepository organizationRepository;

    public AdmissionIdempotencySupport(AdmissionApplicationRepository admissionApplicationRepository,
                                       AdmissionOperationLogRepository admissionOperationLogRepository,
                                       OrganizationRepository organizationRepository) {
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.admissionOperationLogRepository = admissionOperationLogRepository;
        this.organizationRepository = organizationRepository;
    }

    public AdmissionApplicationEntity ensureSameSubmission(AdmissionApplicationEntity existing,
                                                           AdmissionApplicationCommand command) {
        if (Objects.equals(existing.getOrganizationType(), command.organizationType())
                && Objects.equals(existing.getApplicantName(), command.applicantName())
                && Objects.equals(existing.getBusinessLicenseNo(), command.businessLicenseNo())
                && Objects.equals(existing.getContactName(), command.contactName())
                && Objects.equals(existing.getContactMobile(), command.contactMobile())) {
            return existing;
        }
        throw new IllegalStateException("申请编号已存在且载荷不一致: " + command.applicationId());
    }

    public FoundationQueryModels.OrganizationView findRetriedApproval(String applicationId,
                                                                      AdmissionApplicationEntity application,
                                                                      AdmissionReviewCommand command,
                                                                      String requestFingerprint) {
        if (loadIdempotentApplication(applicationId, "APPROVE", requestFingerprint) != null
                || isSameApprovalAlreadyApplied(application, command)) {
            return loadApprovedOrganizationView(applicationId, command.organizationId());
        }
        return null;
    }

    public AdmissionApplicationEntity findRetriedOperation(String applicationId,
                                                           String actionType,
                                                           String requestFingerprint,
                                                           AdmissionApplicationEntity current,
                                                           AdmissionApplicationStatus targetStatus,
                                                           String operatorId,
                                                           String operationReason) {
        AdmissionApplicationEntity logged = loadIdempotentApplication(applicationId, actionType, requestFingerprint);
        if (logged != null) {
            return logged;
        }
        if (current.getApplicationStatus() == targetStatus
                && Objects.equals(current.getReviewedBy(), operatorId)
                && Objects.equals(current.getReviewComment(), operationReason)) {
            return current;
        }
        return null;
    }

    public AdmissionApplicationEntity findRetriedResubmit(String applicationId,
                                                          AdmissionResubmitCommand command,
                                                          AdmissionApplicationEntity current,
                                                          String requestFingerprint) {
        AdmissionApplicationEntity logged = loadIdempotentApplication(applicationId, "RESUBMIT", requestFingerprint);
        if (logged != null) {
            return logged;
        }
        if (current.getApplicationStatus() == AdmissionApplicationStatus.SUBMITTED
                && current.getAggregateVersion() > 1
                && Objects.equals(current.getOrganizationType(), command.organizationType())
                && Objects.equals(current.getApplicantName(), command.applicantName())
                && Objects.equals(current.getBusinessLicenseNo(), command.businessLicenseNo())
                && Objects.equals(current.getContactName(), command.contactName())
                && Objects.equals(current.getContactMobile(), command.contactMobile())
                && current.getReviewComment() == null
                && current.getReviewedBy() == null
                && current.getReviewedAt() == null) {
            return current;
        }
        return null;
    }

    public AdmissionApplicationEntity findRetriedReassign(String applicationId,
                                                          AdmissionReviewAssignmentCommand command,
                                                          AdmissionApplicationEntity current,
                                                          String requestFingerprint) {
        AdmissionApplicationEntity logged = loadIdempotentApplication(applicationId, "REASSIGN_REVIEW", requestFingerprint);
        if (logged != null) {
            return logged;
        }
        if (current.getApplicationStatus() == AdmissionApplicationStatus.UNDER_REVIEW
                && Objects.equals(current.getReviewedBy(), command.reviewerId())
                && Objects.equals(current.getReviewComment(), command.operationReason())) {
            return current;
        }
        return null;
    }

    public void appendOperationLog(String applicationId,
                                   String actionType,
                                   String requestFingerprint,
                                   Long aggregateVersion,
                                   OffsetDateTime operatedAt) {
        admissionOperationLogRepository.save(
                new AdmissionOperationLogEntity(
                        applicationId,
                        actionType,
                        requestFingerprint,
                        aggregateVersion,
                        operatedAt
                )
        );
    }

    public String fingerprint(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                if (!builder.isEmpty()) {
                    builder.append('|');
                }
                builder.append(part == null ? "<null>" : part);
            }
            return HEX_FORMAT.formatHex(digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法生成入驻操作幂等指纹", exception);
        }
    }

    private AdmissionApplicationEntity loadIdempotentApplication(String applicationId,
                                                                 String actionType,
                                                                 String requestFingerprint) {
        return admissionOperationLogRepository
                .findByApplicationIdAndActionTypeAndRequestFingerprint(applicationId, actionType, requestFingerprint)
                .flatMap(log -> admissionApplicationRepository.findById(log.getApplicationId()))
                .orElse(null);
    }

    private FoundationQueryModels.OrganizationView loadApprovedOrganizationView(String applicationId, String organizationId) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalStateException("幂等审批缺少组织主档: " + organizationId));
        if (!Objects.equals(organization.getSourceApplicationId(), applicationId)) {
            throw new IllegalStateException("幂等审批命中的组织主档与申请不匹配: " + organizationId);
        }
        return new FoundationQueryModels.OrganizationView(
                organization.getOrganizationId(),
                organization.getOrganizationType(),
                organization.getOrganizationName(),
                organization.getStatus().name(),
                organization.getDefaultLocale(),
                organization.getSupportedLocales(),
                organization.getSourceApplicationId(),
                organization.getAggregateVersion()
        );
    }

    private boolean isSameApprovalAlreadyApplied(AdmissionApplicationEntity application, AdmissionReviewCommand command) {
        return application.getApplicationStatus() == AdmissionApplicationStatus.APPROVED
                && Objects.equals(application.getReviewedBy(), command.reviewedBy())
                && Objects.equals(application.getReviewComment(), command.reviewComment());
    }
}
