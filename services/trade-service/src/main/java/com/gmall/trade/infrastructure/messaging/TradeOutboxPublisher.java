package com.gmall.trade.infrastructure.messaging;

import com.gmall.trade.infrastructure.persistence.OutboxEventEntity;
import com.gmall.trade.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TradeOutboxPublisher {

    private static final String TRADE_AGGREGATE_TYPE = "Order";

    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;
    private final TransactionOperations transactionOperations;

    @Autowired
    public TradeOutboxPublisher(OutboxEventRepository outboxEventRepository,
                                StreamBridge streamBridge,
                                PlatformTransactionManager transactionManager) {
        this(outboxEventRepository, streamBridge, buildTransactionOperations(transactionManager));
    }

    TradeOutboxPublisher(OutboxEventRepository outboxEventRepository, StreamBridge streamBridge) {
        this(outboxEventRepository, streamBridge, TransactionOperations.withoutTransaction());
    }

    private TradeOutboxPublisher(OutboxEventRepository outboxEventRepository,
                                 StreamBridge streamBridge,
                                 TransactionOperations transactionOperations) {
        this.outboxEventRepository = outboxEventRepository;
        this.streamBridge = streamBridge;
        this.transactionOperations = transactionOperations;
    }

    @Scheduled(fixedDelayString = "${gmall.outbox.publish-interval-ms:1000}")
    public void publishPending() {
        for (OutboxEventEntity event : outboxEventRepository
                .findByPublishedAtIsNullAndAggregateTypeOrderByCreatedAtAsc(TRADE_AGGREGATE_TYPE)) {
            transactionOperations.executeWithoutResult(ignored -> publishSingleEvent(event));
        }
    }

    private void publishSingleEvent(OutboxEventEntity event) {
        boolean sent = streamBridge.send("tradeEvents-out-0", event.getPayload());
        if (!sent) {
            throw new IllegalStateException("trade 事件发送失败");
        }
        event.markPublished(OffsetDateTime.now());
        outboxEventRepository.save(event);
    }

    private static TransactionOperations buildTransactionOperations(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }
}
