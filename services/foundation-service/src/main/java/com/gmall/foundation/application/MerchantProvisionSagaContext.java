package com.gmall.foundation.application;

public record MerchantProvisionSagaContext(
        String organizationId,
        String sellerId,
        String storefrontId
) {
}
