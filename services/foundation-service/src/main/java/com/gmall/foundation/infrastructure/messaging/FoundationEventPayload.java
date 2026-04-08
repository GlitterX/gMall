package com.gmall.foundation.infrastructure.messaging;

public record FoundationEventPayload(
        String organizationId,
        String sellerId,
        String storefrontId,
        String status,
        String operatorId,
        String operationReason
) {
}
