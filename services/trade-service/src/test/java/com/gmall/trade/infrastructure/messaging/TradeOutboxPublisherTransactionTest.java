package com.gmall.trade.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gmall.trade.TradeServiceApplication;
import com.gmall.trade.infrastructure.persistence.OutboxEventEntity;
import com.gmall.trade.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        classes = TradeServiceApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:trade-outbox-tx;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.main.web-application-type=none",
                "spring.cloud.function.ineligible-definitions=foundationEvents",
                "gmall.outbox.publish-interval-ms=600000",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@Import(TradeOutboxPublisherTransactionTest.TestConfig.class)
class TradeOutboxPublisherTransactionTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        StreamBridge streamBridge() {
            return Mockito.mock(StreamBridge.class);
        }

        @Bean("tradeOutboxPublisher")
        @Primary
        TradeOutboxPublisher tradeOutboxPublisher(OutboxEventRepository outboxEventRepository,
                                                  StreamBridge streamBridge,
                                                  PlatformTransactionManager transactionManager) {
            return new ManualTradeOutboxPublisher(outboxEventRepository, streamBridge, transactionManager);
        }
    }

    static class ManualTradeOutboxPublisher extends TradeOutboxPublisher {

        ManualTradeOutboxPublisher(OutboxEventRepository outboxEventRepository,
                                   StreamBridge streamBridge,
                                   PlatformTransactionManager transactionManager) {
            super(outboxEventRepository, streamBridge, transactionManager);
        }

        @Override
        public void publishPending() {
            super.publishPending();
        }
    }

    private static final String FIRST_PAYLOAD = "{\"eventType\":\"OrderSubmitted\",\"aggregateId\":\"order-1\"}";
    private static final String SECOND_PAYLOAD = "{\"eventType\":\"OrderSubmitted\",\"aggregateId\":\"order-2\"}";

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TradeOutboxPublisher tradeOutboxPublisher;

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        Mockito.reset(streamBridge);
    }

    @Test
    void testContextDoesNotLoadUnrelatedMessagingInfrastructure() {
        assertThat(applicationContext.containsBean("foundationEvents-in-0")).isFalse();
        assertThat(AopUtils.getTargetClass(tradeOutboxPublisher)).isEqualTo(ManualTradeOutboxPublisher.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void publishPendingKeepsEarlierPublishedMarkerWhenLaterEventFails() {
        outboxEventRepository.save(new OutboxEventEntity(
                "evt-1",
                "Order",
                "order-1",
                "OrderSubmitted",
                FIRST_PAYLOAD,
                OffsetDateTime.parse("2026-03-31T23:00:00+08:00"),
                null
        ));
        outboxEventRepository.save(new OutboxEventEntity(
                "evt-2",
                "Order",
                "order-2",
                "OrderSubmitted",
                SECOND_PAYLOAD,
                OffsetDateTime.parse("2026-03-31T23:00:01+08:00"),
                null
        ));
        when(streamBridge.send("tradeEvents-out-0", FIRST_PAYLOAD)).thenReturn(true);
        when(streamBridge.send("tradeEvents-out-0", SECOND_PAYLOAD)).thenReturn(false);

        assertThatThrownBy(() -> tradeOutboxPublisher.publishPending())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("trade 事件发送失败");

        assertThat(outboxEventRepository.findById("evt-1"))
                .get()
                .extracting(OutboxEventEntity::getPublishedAt)
                .isNotNull();
        assertThat(outboxEventRepository.findById("evt-2"))
                .get()
                .extracting(OutboxEventEntity::getPublishedAt)
                .isNull();
    }
}
