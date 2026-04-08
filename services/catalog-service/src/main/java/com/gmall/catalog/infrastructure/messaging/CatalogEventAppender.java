package com.gmall.catalog.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.catalog.infrastructure.persistence.OutboxEventEntity;
import com.gmall.catalog.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CatalogEventAppender {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public CatalogEventAppender(OutboxEventRepository outboxEventRepository,
                                ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void append(String aggregateType,
                       String aggregateId,
                       String eventType,
                       long version,
                       CatalogEventPayload payload) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            outboxEventRepository.save(new OutboxEventEntity(
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
                    now,
                    now,
                    null,
                    now,
                    null,
                    null
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法写入 catalog outbox 事件", exception);
        }
    }
}
