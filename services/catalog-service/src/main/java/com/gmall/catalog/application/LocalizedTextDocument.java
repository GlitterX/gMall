package com.gmall.catalog.application;

import java.util.LinkedHashMap;
import java.util.Map;

public record LocalizedTextDocument(String defaultLocale,
                                    String fallbackPolicy,
                                    Map<String, String> translations) {

    public LocalizedTextDocument {
        translations = translations == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(translations));
    }

    public LocalizedTextResolution resolve(String preferredLocale) {
        String resolvedLocale = preferredLocale;
        if (!hasText(resolvedLocale) || !translations.containsKey(resolvedLocale)) {
            resolvedLocale = defaultLocale;
        }
        if (!hasText(resolvedLocale) || !translations.containsKey(resolvedLocale)) {
            resolvedLocale = translations.keySet().stream().findFirst().orElse(null);
        }
        if (!hasText(resolvedLocale)) {
            return new LocalizedTextResolution("", null, true);
        }
        return new LocalizedTextResolution(
                translations.getOrDefault(resolvedLocale, ""),
                resolvedLocale,
                preferredLocale != null && !preferredLocale.equals(resolvedLocale)
        );
    }

    public LocalizedTextDocument mergeOverride(LocalizedTextDocument override) {
        if (override == null) {
            return this;
        }
        Map<String, String> mergedTranslations = new LinkedHashMap<>(translations);
        mergedTranslations.putAll(override.translations());
        String mergedDefaultLocale = hasText(override.defaultLocale()) ? override.defaultLocale() : defaultLocale;
        String mergedFallbackPolicy = hasText(override.fallbackPolicy()) ? override.fallbackPolicy() : fallbackPolicy;
        return new LocalizedTextDocument(mergedDefaultLocale, mergedFallbackPolicy, mergedTranslations);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record LocalizedTextResolution(String value, String resolvedLocale, boolean fallbackApplied) {
    }
}
