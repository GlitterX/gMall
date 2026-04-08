package com.gmall.decoration.infrastructure.messaging;

import com.gmall.decoration.infrastructure.persistence.DecorationOutboxEventEntity;
import com.gmall.decoration.infrastructure.persistence.DecorationOutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DecorationOutboxPublisher {

    private final DecorationOutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;

    @Autowired
    public DecorationOutboxPublisher(DecorationOutboxEventRepository outboxEventRepository,
                                     StreamBridge streamBridge) {
        this.outboxEventRepository = outboxEventRepository;
        this.streamBridge = streamBridge;
    }

    @Scheduled(fixedDelayString = "${gmall.outbox.publish-interval-ms:1000}")
    public void publishPending() {
        List<DecorationOutboxEventEntity> events = outboxEventRepository
                .findByPublishStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List.of("PENDING", "FAILED"),
                        OffsetDateTime.now()
                );
        for (DecorationOutboxEventEntity event : events) {
            boolean sent = streamBridge.send("decorationEvents-out-0", event.getPayload());
            if (sent) {
                event.markPublished(OffsetDateTime.now());
            } else {
                event.markRetry(OffsetDateTime.now().plusSeconds(30));
            }
            outboxEventRepository.save(event);
        }
    }
}
