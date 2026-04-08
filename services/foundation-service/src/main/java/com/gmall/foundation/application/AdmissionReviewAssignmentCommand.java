package com.gmall.foundation.application;

public record AdmissionReviewAssignmentCommand(
        String operatorId,
        String reviewerId,
        String operationReason
) {
}
