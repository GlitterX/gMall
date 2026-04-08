package com.gmall.shared.localization;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalizationResolver {

    public static final String DEFAULT_LOCALE = "zh-CN";
    private static final List<String> DEFAULT_FALLBACK_CHAIN = List.of("en-US", DEFAULT_LOCALE);

    public LocalizationResolution resolve(Map<String, String> translations,
                                          String requestedLocale,
                                          LocalizationFieldPolicy fieldPolicy) {
        LocalizationFieldPolicy effectivePolicy = fieldPolicy == null
                ? LocalizationFieldPolicy.optional("default")
                : fieldPolicy;
        Map<String, String> normalizedTranslations = normalizeTranslations(translations);
        String normalizedRequestedLocale = hasText(requestedLocale) ? requestedLocale : DEFAULT_LOCALE;
        List<String> missingRequiredLocales = effectivePolicy.requiredLocales().stream()
                .filter(locale -> !hasText(normalizedTranslations.get(locale)))
                .toList();

        String resolvedLocale = resolveLocale(normalizedTranslations, normalizedRequestedLocale, effectivePolicy.allowFallback());
        String resolvedValue = hasText(resolvedLocale) ? normalizedTranslations.getOrDefault(resolvedLocale, "") : "";
        boolean fallbackApplied = hasText(resolvedLocale) && !normalizedRequestedLocale.equals(resolvedLocale);
        boolean translationMissing = !hasText(resolvedLocale)
                || (!effectivePolicy.emptyValueAllowed() && !hasText(resolvedValue));
        boolean publishReady = missingRequiredLocales.isEmpty()
                && !translationMissing
                && (effectivePolicy.emptyValueAllowed() || hasText(resolvedValue));

        return new LocalizationResolution(
                normalizedRequestedLocale,
                resolvedLocale,
                fallbackApplied,
                translationMissing,
                resolvedValue,
                missingRequiredLocales,
                publishReady
        );
    }

    private String resolveLocale(Map<String, String> translations, String requestedLocale, boolean allowFallback) {
        if (hasText(translations.get(requestedLocale))) {
            return requestedLocale;
        }
        if (!allowFallback) {
            return null;
        }
        for (String locale : fallbackChain(requestedLocale)) {
            if (hasText(translations.get(locale))) {
                return locale;
            }
        }
        return null;
    }

    private List<String> fallbackChain(String requestedLocale) {
        return DEFAULT_FALLBACK_CHAIN.stream()
                .filter(locale -> !locale.equals(requestedLocale))
                .collect(Collectors.toList());
    }

    private Map<String, String> normalizeTranslations(Map<String, String> translations) {
        if (translations == null || translations.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        translations.forEach((locale, value) -> {
            if (!hasText(locale)) {
                return;
            }
            normalized.put(locale, value == null ? "" : value.trim());
        });
        return Map.copyOf(normalized);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
