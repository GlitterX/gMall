package com.gmall.catalog.infrastructure.messaging;

import com.gmall.catalog.infrastructure.persistence.OutboxEventEntity;
import com.gmall.catalog.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CatalogOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(CatalogOutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;
    private final TransactionOperations transactionOperations;
    private final long retryDelaySeconds;
    private final int maxRetryCount;

    @Autowired
    public CatalogOutboxPublisher(OutboxEventRepository outboxEventRepository,
                                  StreamBridge streamBridge,
                                  PlatformTransactionManager transactionManager,
                                  @Value("${gmall.outbox.retry-delay-seconds:30}") long retryDelaySeconds,
                                  @Value("${gmall.outbox.max-retry-count:5}") int maxRetryCount) {
        this(outboxEventRepository, streamBridge, buildTransactionOperations(transactionManager), retryDelaySeconds, maxRetryCount);
    }

    CatalogOutboxPublisher(OutboxEventRepository outboxEventRepository,
                           StreamBridge streamBridge) {
        this(outboxEventRepository, streamBridge, 30, 5);
    }

    CatalogOutboxPublisher(OutboxEventRepository outboxEventRepository,
                           StreamBridge streamBridge,
                           long retryDelaySeconds,
                           int maxRetryCount) {
        this(outboxEventRepository, streamBridge, TransactionOperations.withoutTransaction(), retryDelaySeconds, maxRetryCount);
    }

    private CatalogOutboxPublisher(OutboxEventRepository outboxEventRepository,
                                   StreamBridge streamBridge,
                                   TransactionOperations transactionOperations,
                                   long retryDelaySeconds,
                                   int maxRetryCount) {
        this.outboxEventRepository = outboxEventRepository;
        this.streamBridge = streamBridge;
        this.transactionOperations = transactionOperations;
        this.retryDelaySeconds = retryDelaySeconds;
        this.maxRetryCount = maxRetryCount;
    }

    @Scheduled(fixedDelayString = "${gmall.outbox.publish-interval-ms:1000}")
    public void publishPending() {
        List<OutboxEventEntity> events = outboxEventRepository
                .findByPublishStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List.of("PENDING", "FAILED"),
                        OffsetDateTime.now()
                );
        for (OutboxEventEntity event : events) {
            transactionOperations.executeWithoutResult(ignored -> publishSingleEvent(event));
        }
    }

    private void publishSingleEvent(OutboxEventEntity event) {
        OffsetDateTime attemptedAt = OffsetDateTime.now();
        try {
            boolean sent = streamBridge.send("catalogEvents-out-0", event.getPayload());
            if (sent) {
                event.markPublished(attemptedAt);
            } else {
                event.markRetry(
                        attemptedAt,
                        attemptedAt.plusSeconds(retryDelaySeconds),
                        maxRetryCount,
                        "streamBridge send 返回 false"
                );
            }
        } catch (RuntimeException exception) {
            event.markRetry(
                    attemptedAt,
                    attemptedAt.plusSeconds(retryDelaySeconds),
                    maxRetryCount,
                    exception.getMessage()
            );
        }
        if ("DEAD_LETTER".equals(event.getPublishStatus())) {
            log.warn("catalog outbox dead-lettered: eventId={}, retryCount={}", event.getEventId(), event.getRetryCount());
        }
        outboxEventRepository.save(event);
    }

    private static TransactionOperations buildTransactionOperations(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }
}
