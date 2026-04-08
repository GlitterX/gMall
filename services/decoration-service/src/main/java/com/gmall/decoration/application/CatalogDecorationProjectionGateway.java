package com.gmall.decoration.application;

import java.util.List;

public interface CatalogDecorationProjectionGateway {

    List<DecorationProduct> getProducts(List<String> productViewIds, String locale);

    record DecorationProduct(String productViewId,
                             String title,
                             String coverImage,
                             String productStatus,
                             String sourceType,
                             String resolvedLocale,
                             boolean fallbackApplied,
                             String contentCompleteness) {
    }
}
