package com.gmall.foundation.infrastructure.messaging;

import java.time.OffsetDateTime;

public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Long version,
        OffsetDateTime occurredAt,
        String idempotencyKey,
        T payload
) {
}
