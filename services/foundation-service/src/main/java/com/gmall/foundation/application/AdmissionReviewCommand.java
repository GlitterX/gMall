package com.gmall.foundation.application;

public record AdmissionReviewCommand(
        String decision,
        String reviewComment,
        String reviewedBy,
        String organizationId,
        String defaultLocale,
        String supportedLocales,
        String admissionChannel,
        String industryCategory,
        String businessScope,
        String remark
) {
}
