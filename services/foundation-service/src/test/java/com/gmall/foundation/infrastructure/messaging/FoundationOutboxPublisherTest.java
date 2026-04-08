package com.gmall.foundation.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmall.foundation.infrastructure.persistence.OutboxEventEntity;
import com.gmall.foundation.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;

class FoundationOutboxPublisherTest {

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final StreamBridge streamBridge = mock(StreamBridge.class);

    @Test
    void publishPendingMarksEventAsPublishedWhenSendSucceeds() {
        OutboxEventEntity event = new OutboxEventEntity(
                "evt-1",
                "Storefront",
                "store-1",
                "StorefrontActivated",
                "{\"eventType\":\"StorefrontActivated\"}",
                OffsetDateTime.parse("2026-03-31T22:00:00+08:00"),
                null
        );
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(streamBridge.send("foundationEvents-out-0", "{\"eventType\":\"StorefrontActivated\"}"))
                .thenReturn(true);

        new FoundationOutboxPublisher(outboxEventRepository, streamBridge).publishPending();

        verify(streamBridge).send("foundationEvents-out-0", "{\"eventType\":\"StorefrontActivated\"}");
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPendingThrowsWhenSendFailsAndKeepsEventUnpublished() {
        OutboxEventEntity event = new OutboxEventEntity(
                "evt-2",
                "Storefront",
                "store-2",
                "StorefrontActivated",
                "{\"eventType\":\"StorefrontActivated\"}",
                OffsetDateTime.parse("2026-03-31T22:00:00+08:00"),
                null
        );
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(streamBridge.send("foundationEvents-out-0", "{\"eventType\":\"StorefrontActivated\"}"))
                .thenReturn(false);

        assertThatThrownBy(() -> new FoundationOutboxPublisher(outboxEventRepository, streamBridge).publishPending())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("foundation 事件发送失败");
        assertThat(event.getPublishedAt()).isNull();
    }
}
