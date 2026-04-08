package com.gmall.foundation.application;

public record AdmissionApplicationCommand(
        String applicationId,
        String organizationType,
        String applicantName,
        String businessLicenseNo,
        String contactName,
        String contactMobile
) {
}
