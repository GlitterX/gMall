package com.gmall.trade.infrastructure.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionEntity;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class FoundationEligibilityConsumer {

    private static final Logger log = LoggerFactory.getLogger(FoundationEligibilityConsumer.class);

    @Bean
    Consumer<Message<String>> foundationEvents(ObjectMapper objectMapper,
                                               EligibilityProjectionRepository eligibilityProjectionRepository) {
        return message -> handleMessage(message, objectMapper, eligibilityProjectionRepository);
    }

    @Transactional
    void handleMessage(Message<String> message,
                       ObjectMapper objectMapper,
                       EligibilityProjectionRepository eligibilityProjectionRepository) {
        try {
            EventEnvelope<FoundationEventPayload> event = readEvent(message.getPayload(), objectMapper);
            log.debug("收到 foundation 事件: type={}, sellerId={}, storefrontId={}, organizationId={}",
                    event.eventType(),
                    event.payload() == null ? null : event.payload().sellerId(),
                    event.payload() == null ? null : event.payload().storefrontId(),
                    event.payload() == null ? null : event.payload().organizationId());
            switch (event.eventType()) {
                case "StorefrontActivated" -> upsertProjection(event, true, eligibilityProjectionRepository);
                case "StorefrontFrozen" -> upsertProjection(event, false, eligibilityProjectionRepository);
                case "SellerSuspended" -> updateSellerStatus(event, "SUSPENDED", eligibilityProjectionRepository);
                case "SellerActivated" -> updateSellerStatus(event, "ACTIVE", eligibilityProjectionRepository);
                case "OrganizationFrozen" -> updateOrganizationStatus(event, "FROZEN", eligibilityProjectionRepository);
                case "OrganizationActivated" -> updateOrganizationStatus(event, "ACTIVE", eligibilityProjectionRepository);
                default -> {
                    // 首轮只消费经营资格相关事件，其他事件先忽略。
                }
            }
        } catch (RuntimeException exception) {
            log.error("消费 foundation 事件失败, payload={}", message.getPayload(), exception);
            throw exception;
        }
    }

    private EventEnvelope<FoundationEventPayload> readEvent(String payload, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("无法解析 foundation 事件", exception);
        }
    }

    private void upsertProjection(EventEnvelope<FoundationEventPayload> event,
                                  boolean eligible,
                                  EligibilityProjectionRepository repository) {
        FoundationEventPayload payload = event.payload();
        Optional<EligibilityProjectionEntity> existing = repository
                .findBySellerIdAndStorefrontId(payload.sellerId(), payload.storefrontId());
        EligibilityProjectionEntity projection = existing.orElseGet(() -> new EligibilityProjectionEntity(
                payload.sellerId(),
                payload.storefrontId(),
                payload.organizationId(),
                eligible,
                OffsetDateTime.now()
        ));
        projection.applyStorefrontStatus(eligible, event.version(), OffsetDateTime.now());
        repository.save(projection);
    }

    private void updateSellerStatus(EventEnvelope<FoundationEventPayload> event,
                                    String status,
                                    EligibilityProjectionRepository repository) {
        List<EligibilityProjectionEntity> projections = repository.findBySellerId(event.payload().sellerId());
        for (EligibilityProjectionEntity projection : projections) {
            projection.applySellerStatus(status, event.version(), OffsetDateTime.now());
            repository.save(projection);
        }
    }

    private void updateOrganizationStatus(EventEnvelope<FoundationEventPayload> event,
                                          String status,
                                          EligibilityProjectionRepository repository) {
        List<EligibilityProjectionEntity> projections = repository.findByOrganizationId(event.payload().organizationId());
        for (EligibilityProjectionEntity projection : projections) {
            projection.applyOrganizationStatus(status, event.version(), OffsetDateTime.now());
            repository.save(projection);
        }
    }
}
