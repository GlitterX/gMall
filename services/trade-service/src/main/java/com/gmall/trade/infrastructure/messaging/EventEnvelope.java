package com.gmall.trade.infrastructure.messaging;

import java.time.OffsetDateTime;

public record EventEnvelope<T>(String eventId,
                               String eventType,
                               String aggregateType,
                               String aggregateId,
                               long version,
                               OffsetDateTime occurredAt,
                               String idempotencyKey,
                               T payload) {
}
