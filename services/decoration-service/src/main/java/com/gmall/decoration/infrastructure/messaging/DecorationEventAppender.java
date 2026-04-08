package com.gmall.decoration.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.decoration.infrastructure.persistence.DecorationOutboxEventEntity;
import com.gmall.decoration.infrastructure.persistence.DecorationOutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DecorationEventAppender {

    private final DecorationOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public DecorationEventAppender(DecorationOutboxEventRepository outboxEventRepository,
                                   ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void append(String aggregateType,
                       String aggregateId,
                       String eventType,
                       long version,
                       Map<String, Object> payload) {
        try {
            outboxEventRepository.save(new DecorationOutboxEventEntity(
                    UUID.randomUUID().toString(),
                    aggregateType,
                    aggregateId,
                    version,
                    eventType,
                    objectMapper.writeValueAsString(new EventEnvelope<>(
                            UUID.randomUUID().toString(),
                            eventType,
                            aggregateType,
                            aggregateId,
                            version,
                            OffsetDateTime.now(),
                            UUID.randomUUID().toString(),
                            payload
                    )),
                    "PENDING",
                    0,
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    null
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法写入 decoration outbox 事件", exception);
        }
    }
}
