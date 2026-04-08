package com.gmall.shared.localization;

public record LocaleMeta(
    String requestedLocale,
    String resolvedLocale,
    boolean fallbackApplied
) {
}
