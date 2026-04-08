package com.gmall.trade.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmall.trade.infrastructure.persistence.OutboxEventEntity;
import com.gmall.trade.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;

class TradeOutboxPublisherTest {

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final StreamBridge streamBridge = mock(StreamBridge.class);

    @Test
    void publishPendingMarksRejectedEventAsPublishedWhenSendSucceeds() {
        OutboxEventEntity event = new OutboxEventEntity(
                "evt-1",
                "Order",
                "biz-rejected",
                "OrderRejected",
                "{\"eventType\":\"OrderRejected\"}",
                OffsetDateTime.parse("2026-03-31T22:00:00+08:00"),
                null
        );
        when(outboxEventRepository.findByPublishedAtIsNullAndAggregateTypeOrderByCreatedAtAsc("Order"))
                .thenReturn(List.of(event));
        when(streamBridge.send("tradeEvents-out-0", "{\"eventType\":\"OrderRejected\"}"))
                .thenReturn(true);

        new TradeOutboxPublisher(outboxEventRepository, streamBridge).publishPending();

        verify(streamBridge).send("tradeEvents-out-0", "{\"eventType\":\"OrderRejected\"}");
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPendingThrowsWhenSendFailsAndKeepsEventUnpublished() {
        OutboxEventEntity event = new OutboxEventEntity(
                "evt-2",
                "Order",
                "biz-rejected",
                "OrderRejected",
                "{\"eventType\":\"OrderRejected\"}",
                OffsetDateTime.parse("2026-03-31T22:00:00+08:00"),
                null
        );
        when(outboxEventRepository.findByPublishedAtIsNullAndAggregateTypeOrderByCreatedAtAsc("Order"))
                .thenReturn(List.of(event));
        when(streamBridge.send("tradeEvents-out-0", "{\"eventType\":\"OrderRejected\"}"))
                .thenReturn(false);

        assertThatThrownBy(() -> new TradeOutboxPublisher(outboxEventRepository, streamBridge).publishPending())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("trade 事件发送失败");
        assertThat(event.getPublishedAt()).isNull();
    }
}
