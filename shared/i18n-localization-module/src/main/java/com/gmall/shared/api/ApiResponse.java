package com.gmall.shared.api;

import com.gmall.shared.localization.LocaleMeta;

public record ApiResponse<T>(
    boolean success,
    ErrorCode errorCode,
    String message,
    T data,
    LocaleMeta localeMeta
) {

    public static <T> ApiResponse<T> success(T data, LocaleMeta localeMeta) {
        return new ApiResponse<>(true, ErrorCode.SUCCESS, "ok", data, localeMeta);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null, null);
    }
}
