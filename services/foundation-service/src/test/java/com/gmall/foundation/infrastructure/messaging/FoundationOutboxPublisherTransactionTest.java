package com.gmall.foundation.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gmall.foundation.FoundationServiceApplication;
import com.gmall.foundation.infrastructure.persistence.OutboxEventEntity;
import com.gmall.foundation.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        classes = FoundationServiceApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:foundation-outbox-tx;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.main.web-application-type=none",
                "gmall.outbox.publish-interval-ms=600000",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@Import(FoundationOutboxPublisherTransactionTest.TestConfig.class)
class FoundationOutboxPublisherTransactionTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        StreamBridge streamBridge() {
            return Mockito.mock(StreamBridge.class);
        }
    }

    private static final String FIRST_PAYLOAD = "{\"eventType\":\"OrganizationActivated\",\"aggregateId\":\"org-1\"}";
    private static final String SECOND_PAYLOAD = "{\"eventType\":\"SellerActivated\",\"aggregateId\":\"seller-1\"}";

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private FoundationOutboxPublisher foundationOutboxPublisher;

    @Autowired
    private StreamBridge streamBridge;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        Mockito.reset(streamBridge);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void publishPendingKeepsEarlierPublishedMarkerWhenLaterEventFails() {
        outboxEventRepository.save(new OutboxEventEntity(
                "evt-1",
                "Organization",
                "org-1",
                "OrganizationActivated",
                FIRST_PAYLOAD,
                OffsetDateTime.parse("2026-03-31T23:00:00+08:00"),
                null
        ));
        outboxEventRepository.save(new OutboxEventEntity(
                "evt-2",
                "Seller",
                "seller-1",
                "SellerActivated",
                SECOND_PAYLOAD,
                OffsetDateTime.parse("2026-03-31T23:00:01+08:00"),
                null
        ));
        when(streamBridge.send("foundationEvents-out-0", FIRST_PAYLOAD)).thenReturn(true);
        when(streamBridge.send("foundationEvents-out-0", SECOND_PAYLOAD)).thenReturn(false);

        assertThatThrownBy(() -> foundationOutboxPublisher.publishPending())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("foundation 事件发送失败");

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
