package com.gmall.foundation.application;

public record AdmissionResubmitCommand(
        String applicationId,
        String organizationType,
        String applicantName,
        String businessLicenseNo,
        String contactName,
        String contactMobile,
        String operatorId,
        String operationReason
) {
}
