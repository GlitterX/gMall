package com.gmall.catalog.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.catalog.infrastructure.persistence.CatalogProjectionEntity;
import com.gmall.catalog.infrastructure.persistence.CatalogProjectionRepository;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductEntity;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductRepository;
import com.gmall.catalog.infrastructure.persistence.SourceProductEntity;
import com.gmall.catalog.infrastructure.persistence.SourceProductRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;

class CatalogProjectionConsumerTest {

    private final SourceProductRepository sourceProductRepository = mock(SourceProductRepository.class);
    private final MerchantOfferProductRepository merchantOfferProductRepository = mock(MerchantOfferProductRepository.class);
    private final CatalogProjectionRepository catalogProjectionRepository = mock(CatalogProjectionRepository.class);

    private final List<CatalogProjectionEntity> savedProjections = new ArrayList<>();

    private CatalogProjectionConsumer catalogProjectionConsumer;

    @BeforeEach
    void setUp() {
        catalogProjectionConsumer = new CatalogProjectionConsumer();
        when(catalogProjectionRepository.save(any(CatalogProjectionEntity.class))).thenAnswer(invocation -> {
            CatalogProjectionEntity entity = invocation.getArgument(0);
            savedProjections.add(entity);
            return entity;
        });
        when(catalogProjectionRepository.findByPresentationTypeAndProductViewIdAndLocale(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void sourceProductCreatedRebuildsMallAndDecorationProjection() throws Exception {
        when(sourceProductRepository.findById("source-1")).thenReturn(Optional.of(new SourceProductEntity(
                "source-1",
                "view-source-1",
                "SUPPLIER",
                "org-supplier",
                "SUPPLY",
                "cat-fruit",
                null,
                """
                {"productName":{"defaultLocale":"zh-CN","fallbackPolicy":"DEFAULT_LOCALE","translations":{"zh-CN":"有机草莓礼盒"}},"coverImage":"https://img.example.com/source.png"}
                """,
                "ACTIVE",
                1L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        )));

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String payload = objectMapper.writeValueAsString(new EventEnvelope<>(
                "event-1",
                "SourceProductCreated",
                "SourceProduct",
                "source-1",
                1L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                "trace-1",
                new CatalogEventPayload("source-1", null, "view-source-1", null, null)
        ));

        catalogProjectionConsumer.handleMessage(
                MessageBuilder.withPayload(payload).build(),
                objectMapper,
                sourceProductRepository,
                merchantOfferProductRepository,
                catalogProjectionRepository
        );

        assertThat(savedProjections).hasSize(2);
        assertThat(savedProjections)
                .extracting(CatalogProjectionEntity::getPresentationType)
                .containsExactlyInAnyOrder("MALL", "DECORATION");
    }

    @Test
    void merchantOfferProductCreatedUsesOfferContentOverride() throws Exception {
        when(sourceProductRepository.findById("source-2")).thenReturn(Optional.of(new SourceProductEntity(
                "source-2",
                "view-source-2",
                "SUPPLIER",
                "org-supplier",
                "SUPPLY",
                "cat-fruit",
                null,
                """
                {"productName":{"defaultLocale":"zh-CN","fallbackPolicy":"DEFAULT_LOCALE","translations":{"zh-CN":"源商品标题"}},"coverImage":"https://img.example.com/source.png"}
                """,
                "ACTIVE",
                1L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        )));
        when(merchantOfferProductRepository.findById("offer-2")).thenReturn(Optional.of(new MerchantOfferProductEntity(
                "offer-2",
                "view-offer-2",
                "merchant-2",
                "rel-2",
                "source-2",
                """
                {"productName":{"defaultLocale":"zh-CN","fallbackPolicy":"DEFAULT_LOCALE","translations":{"zh-CN":"经营商品标题"}},"coverImage":"https://img.example.com/offer.png"}
                """,
                "ACTIVE",
                "SYNCED",
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        )));

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String payload = objectMapper.writeValueAsString(new EventEnvelope<>(
                "event-2",
                "MerchantOfferProductCreated",
                "MerchantOfferProduct",
                "offer-2",
                1L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                "trace-2",
                new CatalogEventPayload("source-2", "offer-2", "view-offer-2", "merchant-2", "rel-2")
        ));

        catalogProjectionConsumer.handleMessage(
                MessageBuilder.withPayload(payload).build(),
                objectMapper,
                sourceProductRepository,
                merchantOfferProductRepository,
                catalogProjectionRepository
        );

        assertThat(savedProjections).hasSize(2);
        assertThat(savedProjections.get(0).getProjectionPayload()).contains("经营商品标题");
    }

    @Test
    void sourceProductUpdatedRebuildsDerivedMerchantOfferProjection() throws Exception {
        when(sourceProductRepository.findById("source-3")).thenReturn(Optional.of(new SourceProductEntity(
                "source-3",
                "view-source-3",
                "SUPPLIER",
                "org-supplier",
                "SUPPLY",
                "cat-fruit",
                null,
                """
                {"productName":{"defaultLocale":"zh-CN","fallbackPolicy":"DEFAULT_LOCALE","translations":{"zh-CN":"源商品新标题"}},"coverImage":"https://img.example.com/source-new.png"}
                """,
                "ON_SHELF",
                2L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        )));
        MerchantOfferProductEntity offerProduct = new MerchantOfferProductEntity(
                "offer-3",
                "view-offer-3",
                "merchant-3",
                "rel-3",
                "source-3",
                """
                {"productName":{"defaultLocale":"zh-CN","fallbackPolicy":"DEFAULT_LOCALE","translations":{"zh-CN":"经营商品旧标题"}},"coverImage":"https://img.example.com/offer.png"}
                """,
                "INVALID_PENDING_CONFIRM",
                "PENDING_CONFIRM",
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        );
        when(merchantOfferProductRepository.findBySourceProductId("source-3")).thenReturn(List.of(offerProduct));
        when(merchantOfferProductRepository.findById("offer-3")).thenReturn(Optional.of(offerProduct));

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String payload = objectMapper.writeValueAsString(new EventEnvelope<>(
                "event-3",
                "SourceProductUpdated",
                "SourceProduct",
                "source-3",
                2L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                "trace-3",
                new CatalogEventPayload("source-3", null, "view-source-3", null, null)
        ));

        catalogProjectionConsumer.handleMessage(
                MessageBuilder.withPayload(payload).build(),
                objectMapper,
                sourceProductRepository,
                merchantOfferProductRepository,
                catalogProjectionRepository
        );

        assertThat(savedProjections).hasSize(4);
        assertThat(savedProjections)
                .extracting(CatalogProjectionEntity::getProductViewId)
                .contains("view-source-3", "view-offer-3");
        assertThat(savedProjections)
                .extracting(CatalogProjectionEntity::getProjectionPayload)
                .anyMatch(item -> item.contains("INVALID_PENDING_CONFIRM"));
    }
}
