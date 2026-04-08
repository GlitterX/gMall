package com.gmall.catalog.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmall.catalog.infrastructure.persistence.OutboxEventEntity;
import com.gmall.catalog.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;

class CatalogOutboxPublisherTest {

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final StreamBridge streamBridge = mock(StreamBridge.class);

    private final List<OutboxEventEntity> savedEvents = new ArrayList<>();

    @Test
    void publishPendingMarksEventAsSentWhenPublishSucceeded() {
        OutboxEventEntity event = new OutboxEventEntity(
                "event-1",
                "SourceProduct",
                "source-1",
                1L,
                "SourceProductCreated",
                "{\"eventType\":\"SourceProductCreated\"}",
                "PENDING",
                0,
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                null,
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                null,
                null
        );
        when(outboxEventRepository.findByPublishStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(anyList(), any()))
                .thenReturn(List.of(event));
        when(outboxEventRepository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> {
            OutboxEventEntity saved = invocation.getArgument(0);
            savedEvents.add(saved);
            return saved;
        });
        when(streamBridge.send("catalogEvents-out-0", event.getPayload())).thenReturn(true);

        CatalogOutboxPublisher publisher = new CatalogOutboxPublisher(outboxEventRepository, streamBridge);
        publisher.publishPending();

        assertThat(savedEvents).singleElement().satisfies(saved -> {
            assertThat(saved.getPublishStatus()).isEqualTo("SENT");
            assertThat(saved.getRetryCount()).isEqualTo(0);
            assertThat(saved.getPublishedAt()).isNotNull();
            assertThat(saved.getNextRetryAt()).isNull();
            assertThat(saved.getLastAttemptAt()).isNotNull();
            assertThat(saved.getLastErrorMessage()).isNull();
        });
    }

    @Test
    void publishPendingMarksEventAsFailedWhenPublishReturnedFalse() {
        OutboxEventEntity event = new OutboxEventEntity(
                "event-2",
                "SourceProduct",
                "source-2",
                2L,
                "SourceProductUpdated",
                "{\"eventType\":\"SourceProductUpdated\"}",
                "PENDING",
                0,
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                null,
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                null,
                null
        );
        when(outboxEventRepository.findByPublishStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(anyList(), any()))
                .thenReturn(List.of(event));
        when(outboxEventRepository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> {
            OutboxEventEntity saved = invocation.getArgument(0);
            savedEvents.add(saved);
            return saved;
        });
        when(streamBridge.send("catalogEvents-out-0", event.getPayload())).thenReturn(false);

        CatalogOutboxPublisher publisher = new CatalogOutboxPublisher(outboxEventRepository, streamBridge);
        publisher.publishPending();

        assertThat(savedEvents).singleElement().satisfies(saved -> {
            assertThat(saved.getPublishStatus()).isEqualTo("FAILED");
            assertThat(saved.getRetryCount()).isEqualTo(1);
            assertThat(saved.getPublishedAt()).isNull();
            assertThat(saved.getNextRetryAt()).isNotNull();
            assertThat(saved.getLastErrorMessage()).contains("streamBridge send 返回 false");
            assertThat(saved.getDeadLetteredAt()).isNull();
        });
    }

    @Test
    void publishPendingMarksEventAsDeadLetterWhenRetryCountReachedMax() {
        OutboxEventEntity event = new OutboxEventEntity(
                "event-3",
                "SourceProduct",
                "source-3",
                2L,
                "SourceProductUpdated",
                "{\"eventType\":\"SourceProductUpdated\"}",
                "FAILED",
                2,
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                null,
                OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                "last-error",
                null
        );
        when(outboxEventRepository.findByPublishStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(anyList(), any()))
                .thenReturn(List.of(event));
        when(outboxEventRepository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> {
            OutboxEventEntity saved = invocation.getArgument(0);
            savedEvents.add(saved);
            return saved;
        });
        when(streamBridge.send("catalogEvents-out-0", event.getPayload())).thenReturn(false);

        CatalogOutboxPublisher publisher = new CatalogOutboxPublisher(outboxEventRepository, streamBridge, 30, 3);
        publisher.publishPending();

        assertThat(savedEvents).singleElement().satisfies(saved -> {
            assertThat(saved.getPublishStatus()).isEqualTo("DEAD_LETTER");
            assertThat(saved.getRetryCount()).isEqualTo(3);
            assertThat(saved.getPublishedAt()).isNull();
            assertThat(saved.getNextRetryAt()).isNull();
            assertThat(saved.getLastErrorMessage()).contains("streamBridge send 返回 false");
            assertThat(saved.getDeadLetteredAt()).isNotNull();
        });
    }
}
