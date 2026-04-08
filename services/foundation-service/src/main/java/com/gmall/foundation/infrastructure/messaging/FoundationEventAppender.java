package com.gmall.foundation.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.foundation.infrastructure.persistence.OutboxEventEntity;
import com.gmall.foundation.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FoundationEventAppender {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public FoundationEventAppender(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void append(String aggregateType,
                       String aggregateId,
                       String eventType,
                       long version,
                       FoundationEventPayload payload) {
        try {
            outboxEventRepository.save(
                    new OutboxEventEntity(
                            UUID.randomUUID().toString(),
                            aggregateType,
                            aggregateId,
                            eventType,
                            objectMapper.writeValueAsString(
                                    new EventEnvelope<>(
                                            UUID.randomUUID().toString(),
                                            eventType,
                                            aggregateType,
                                            aggregateId,
                                            version,
                                            OffsetDateTime.now(),
                                            UUID.randomUUID().toString(),
                                            payload
                                    )
                            ),
                            OffsetDateTime.now(),
                            null
                    )
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法写入 outbox 事件", exception);
        }
    }
}
