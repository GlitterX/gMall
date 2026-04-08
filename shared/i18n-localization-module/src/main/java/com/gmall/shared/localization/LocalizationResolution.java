package com.gmall.shared.localization;

import java.util.List;

public record LocalizationResolution(String requestedLocale,
                                     String resolvedLocale,
                                     boolean fallbackApplied,
                                     boolean translationMissing,
                                     String resolvedValue,
                                     List<String> missingLocales,
                                     boolean publishReady) {

    public LocalizationResolution {
        missingLocales = missingLocales == null ? List.of() : List.copyOf(missingLocales);
    }

    public LocalizedFieldPayload toFieldPayload() {
        return new LocalizedFieldPayload(
                requestedLocale,
                resolvedLocale,
                fallbackApplied,
                translationMissing,
                resolvedValue
        );
    }
}
