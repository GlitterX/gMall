package com.gmall.shared.localization;

import java.util.List;

public record LocalizationFieldPolicy(String fieldCode,
                                      List<String> requiredLocales,
                                      boolean allowFallback,
                                      boolean emptyValueAllowed) {

    public LocalizationFieldPolicy {
        if (fieldCode == null || fieldCode.isBlank()) {
            throw new IllegalArgumentException("fieldCode 不能为空");
        }
        requiredLocales = requiredLocales == null ? List.of() : List.copyOf(requiredLocales);
    }

    public static LocalizationFieldPolicy required(String fieldCode) {
        return new LocalizationFieldPolicy(fieldCode, List.of("zh-CN", "en-US"), true, false);
    }

    public static LocalizationFieldPolicy optional(String fieldCode) {
        return new LocalizationFieldPolicy(fieldCode, List.of(), true, true);
    }
}
