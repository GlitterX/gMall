package com.gmall.trade.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionEntity;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;

class FoundationEligibilityConsumerTest {

    private final EligibilityProjectionRepository eligibilityProjectionRepository = mock(EligibilityProjectionRepository.class);
    private final Map<String, EligibilityProjectionEntity> projections = new HashMap<>();
    private final FoundationEligibilityConsumer consumer = new FoundationEligibilityConsumer();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId(anyString(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        projections.get(invocation.getArgument(0) + "::" + invocation.getArgument(1))
                ));
        when(eligibilityProjectionRepository.findBySellerId(anyString()))
                .thenAnswer(invocation -> filterBySeller(invocation.getArgument(0)));
        when(eligibilityProjectionRepository.findByOrganizationId(anyString()))
                .thenAnswer(invocation -> filterByOrganization(invocation.getArgument(0)));
        when(eligibilityProjectionRepository.save(any(EligibilityProjectionEntity.class)))
                .thenAnswer(invocation -> {
                    EligibilityProjectionEntity projection = invocation.getArgument(0);
                    projections.put(projection.getSellerId() + "::" + projection.getStorefrontId(), projection);
                    return projection;
                });
    }

    @Test
    void handleMessageCreatesEligibleProjectionWhenStorefrontActivated() throws Exception {
        EventEnvelope<FoundationEventPayload> event = new EventEnvelope<>(
                "evt-1",
                "StorefrontActivated",
                "Storefront",
                "store-1",
                1L,
                OffsetDateTime.parse("2026-03-31T22:00:00+08:00"),
                "idem-1",
                new FoundationEventPayload("org-1", "seller-1", "store-1", "ACTIVE", "tester", "activate")
        );

        consumer.handleMessage(
                MessageBuilder.withPayload(objectMapper.writeValueAsString(event)).build(),
                objectMapper,
                eligibilityProjectionRepository
        );

        assertThat(projections)
                .containsKey("seller-1::store-1");
        assertThat(projections.get("seller-1::store-1").getOrganizationId()).isEqualTo("org-1");
        assertThat(projections.get("seller-1::store-1").isEligible()).isTrue();
    }

    @Test
    void handleMessageRestoresEligibilityAndIgnoresOlderStorefrontEvents() throws Exception {
        consumer.handleMessage(
                MessageBuilder.withPayload(objectMapper.writeValueAsString(new EventEnvelope<>(
                        "evt-1",
                        "StorefrontActivated",
                        "Storefront",
                        "store-1",
                        1L,
                        OffsetDateTime.parse("2026-03-31T22:00:00+08:00"),
                        "idem-1",
                        new FoundationEventPayload("org-1", "seller-1", "store-1", "ACTIVE", "tester", "activate")
                ))).build(),
                objectMapper,
                eligibilityProjectionRepository
        );
        consumer.handleMessage(
                MessageBuilder.withPayload(objectMapper.writeValueAsString(new EventEnvelope<>(
                        "evt-2",
                        "OrganizationFrozen",
                        "Organization",
                        "org-1",
                        2L,
                        OffsetDateTime.parse("2026-03-31T22:01:00+08:00"),
                        "idem-2",
                        new FoundationEventPayload("org-1", null, null, "FROZEN", "tester", "freeze")
                ))).build(),
                objectMapper,
                eligibilityProjectionRepository
        );
        consumer.handleMessage(
                MessageBuilder.withPayload(objectMapper.writeValueAsString(new EventEnvelope<>(
                        "evt-3",
                        "OrganizationActivated",
                        "Organization",
                        "org-1",
                        3L,
                        OffsetDateTime.parse("2026-03-31T22:02:00+08:00"),
                        "idem-3",
                        new FoundationEventPayload("org-1", null, null, "ACTIVE", "tester", "restore")
                ))).build(),
                objectMapper,
                eligibilityProjectionRepository
        );
        consumer.handleMessage(
                MessageBuilder.withPayload(objectMapper.writeValueAsString(new EventEnvelope<>(
                        "evt-4",
                        "StorefrontFrozen",
                        "Storefront",
                        "store-1",
                        4L,
                        OffsetDateTime.parse("2026-03-31T22:03:00+08:00"),
                        "idem-4",
                        new FoundationEventPayload("org-1", "seller-1", "store-1", "FROZEN", "tester", "freeze")
                ))).build(),
                objectMapper,
                eligibilityProjectionRepository
        );
        consumer.handleMessage(
                MessageBuilder.withPayload(objectMapper.writeValueAsString(new EventEnvelope<>(
                        "evt-5",
                        "StorefrontActivated",
                        "Storefront",
                        "store-1",
                        3L,
                        OffsetDateTime.parse("2026-03-31T22:02:30+08:00"),
                        "idem-5",
                        new FoundationEventPayload("org-1", "seller-1", "store-1", "ACTIVE", "tester", "older")
                ))).build(),
                objectMapper,
                eligibilityProjectionRepository
        );
        consumer.handleMessage(
                MessageBuilder.withPayload(objectMapper.writeValueAsString(new EventEnvelope<>(
                        "evt-6",
                        "StorefrontActivated",
                        "Storefront",
                        "store-1",
                        5L,
                        OffsetDateTime.parse("2026-03-31T22:04:00+08:00"),
                        "idem-6",
                        new FoundationEventPayload("org-1", "seller-1", "store-1", "ACTIVE", "tester", "restore")
                ))).build(),
                objectMapper,
                eligibilityProjectionRepository
        );
        consumer.handleMessage(
                MessageBuilder.withPayload(objectMapper.writeValueAsString(new EventEnvelope<>(
                        "evt-7",
                        "StorefrontFrozen",
                        "Storefront",
                        "store-1",
                        4L,
                        OffsetDateTime.parse("2026-03-31T22:03:30+08:00"),
                        "idem-7",
                        new FoundationEventPayload("org-1", "seller-1", "store-1", "FROZEN", "tester", "late-old")
                ))).build(),
                objectMapper,
                eligibilityProjectionRepository
        );

        assertThat(projections.get("seller-1::store-1").isEligible()).isTrue();
    }

    private List<EligibilityProjectionEntity> filterBySeller(String sellerId) {
        List<EligibilityProjectionEntity> filtered = new ArrayList<>();
        for (EligibilityProjectionEntity projection : projections.values()) {
            if (projection.getSellerId().equals(sellerId)) {
                filtered.add(projection);
            }
        }
        return filtered;
    }

    private List<EligibilityProjectionEntity> filterByOrganization(String organizationId) {
        List<EligibilityProjectionEntity> filtered = new ArrayList<>();
        for (EligibilityProjectionEntity projection : projections.values()) {
            if (projection.getOrganizationId().equals(organizationId)) {
                filtered.add(projection);
            }
        }
        return filtered;
    }
}
