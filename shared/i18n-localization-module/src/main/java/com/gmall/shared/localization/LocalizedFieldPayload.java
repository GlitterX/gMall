package com.gmall.shared.localization;

public record LocalizedFieldPayload(String requestedLocale,
                                    String resolvedLocale,
                                    boolean fallbackApplied,
                                    boolean translationMissing,
                                    String value) {
}
