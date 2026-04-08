package com.gmall.foundation.application;

public record MerchantProvisionCommand(
        String organizationId,
        String sellerId,
        String storefrontId,
        String organizationType,
        String sellerType,
        String defaultLocale,
        String supportedLocales,
        String operatorId,
        String operationReason
) {
}
