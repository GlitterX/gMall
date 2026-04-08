package com.gmall.catalog.infrastructure.messaging;

public record CatalogEventPayload(String sourceProductId,
                                  String merchantOfferProductId,
                                  String productViewId,
                                  String merchantId,
                                  String relationId) {
}
