package com.gmall.catalog.application;

public record CatalogProjectionPayload(String title,
                                       String coverImage,
                                       String productStatus,
                                       String sourceType,
                                       String resolvedLocale,
                                       boolean fallbackApplied,
                                       String contentCompleteness,
                                       String productType) {
}
