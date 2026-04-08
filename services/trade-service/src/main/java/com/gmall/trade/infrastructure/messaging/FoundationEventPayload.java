package com.gmall.trade.infrastructure.messaging;

public record FoundationEventPayload(String organizationId,
                                     String sellerId,
                                     String storefrontId,
                                     String status,
                                     String operatorId,
                                     String operationReason) {
}
