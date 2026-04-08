package com.gmall.foundation.application;

public record MerchantProvisionResult(
        String organizationId,
        String sellerId,
        String storefrontId
) {
}
