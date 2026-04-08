package com.gmall.decoration.infrastructure.messaging;

import java.time.OffsetDateTime;

public record EventEnvelope<T>(String eventId,
                               String eventType,
                               String aggregateType,
                               String aggregateId,
                               long aggregateVersion,
                               OffsetDateTime occurredAt,
                               String traceId,
                               T payload) {
}
