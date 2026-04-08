package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.AdmissionApplicationStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.messaging.FoundationEventPayload;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewRecordEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewRecordRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class AdmissionWorkflowSupport {

    private static final long REVIEW_TIMEOUT_HOURS = 24L;

    private final AdmissionReviewRecordRepository admissionReviewRecordRepository;
    private final FoundationEventAppender foundationEventAppender;

    public AdmissionWorkflowSupport(AdmissionReviewRecordRepository admissionReviewRecordRepository,
                                    FoundationEventAppender foundationEventAppender) {
        this.admissionReviewRecordRepository = admissionReviewRecordRepository;
        this.foundationEventAppender = foundationEventAppender;
    }

    public void appendReviewRecord(AdmissionApplicationEntity application,
                                   String fromStatus,
                                   String actionType,
                                   String operatorId,
                                   String operationReason,
                                   OffsetDateTime operatedAt) {
        admissionReviewRecordRepository.save(
                new AdmissionReviewRecordEntity(
                        application.getApplicationId(),
                        fromStatus,
                        application.getApplicationStatus().name(),
                        actionType,
                        operatorId,
                        operationReason,
                        operatedAt,
                        application.getAggregateVersion()
                )
        );
    }

    public void ensureUnderReview(AdmissionApplicationEntity application, String message) {
        if (application.getApplicationStatus() != AdmissionApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException(message + ": " + application.getApplicationStatus().name());
        }
    }

    public void ensureTimeoutReviewable(AdmissionApplicationEntity application, OffsetDateTime operatedAt) {
        ensureUnderReview(application, "当前状态不允许审核超时退回");
        OffsetDateTime reviewedAt = application.getReviewedAt();
        if (reviewedAt == null) {
            throw new IllegalStateException("当前审核缺少起算时间，不能判定超时");
        }
        if (reviewedAt.plusHours(REVIEW_TIMEOUT_HOURS).isAfter(operatedAt)) {
            throw new IllegalStateException("当前审核未超时，不能退回待分配");
        }
    }

    public void publishRejectedEvent(AdmissionApplicationEntity application,
                                     String operatorId,
                                     String operationReason) {
        foundationEventAppender.append(
                "AdmissionApplication",
                application.getApplicationId(),
                "OrganizationAdmissionRejected",
                application.getAggregateVersion(),
                new FoundationEventPayload(
                        null,
                        null,
                        null,
                        application.getApplicationStatus().name(),
                        operatorId,
                        operationReason
                )
        );
    }
}
