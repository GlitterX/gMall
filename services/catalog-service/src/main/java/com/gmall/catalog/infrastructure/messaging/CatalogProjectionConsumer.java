package com.gmall.catalog.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.catalog.application.CatalogProjectionPayload;
import com.gmall.catalog.application.LocalizedTextDocument;
import com.gmall.catalog.application.ProductContentDocument;
import com.gmall.catalog.infrastructure.persistence.CatalogProjectionEntity;
import com.gmall.catalog.infrastructure.persistence.CatalogProjectionRepository;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductEntity;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductRepository;
import com.gmall.catalog.infrastructure.persistence.SourceProductEntity;
import com.gmall.catalog.infrastructure.persistence.SourceProductRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class CatalogProjectionConsumer {

    @Bean
    Consumer<Message<String>> catalogEvents(ObjectMapper objectMapper,
                                            SourceProductRepository sourceProductRepository,
                                            MerchantOfferProductRepository merchantOfferProductRepository,
                                            CatalogProjectionRepository catalogProjectionRepository) {
        return message -> handleMessage(
                message,
                objectMapper,
                sourceProductRepository,
                merchantOfferProductRepository,
                catalogProjectionRepository
        );
    }

    @Transactional
    void handleMessage(Message<String> message,
                       ObjectMapper objectMapper,
                       SourceProductRepository sourceProductRepository,
                       MerchantOfferProductRepository merchantOfferProductRepository,
                       CatalogProjectionRepository catalogProjectionRepository) {
        EventEnvelope<CatalogEventPayload> event = readEvent(message.getPayload(), objectMapper);
        switch (event.eventType()) {
            case "SourceProductCreated", "SourceProductUpdated" -> {
                    rebuildSourceProjection(event.payload().sourceProductId(), objectMapper, sourceProductRepository,
                            catalogProjectionRepository);
                    for (MerchantOfferProductEntity offerProduct : merchantOfferProductRepository
                            .findBySourceProductId(event.payload().sourceProductId())) {
                        rebuildMerchantOfferProjection(offerProduct.getMerchantOfferProductId(), objectMapper,
                                sourceProductRepository, merchantOfferProductRepository, catalogProjectionRepository);
                    }
            }
            case "MerchantOfferProductCreated", "MerchantOfferProductUpdated" ->
                    rebuildMerchantOfferProjection(event.payload().merchantOfferProductId(), objectMapper,
                            sourceProductRepository, merchantOfferProductRepository, catalogProjectionRepository);
            default -> {
                // 首轮只消费商品写模型相关事件。
            }
        }
    }

    private EventEnvelope<CatalogEventPayload> readEvent(String payload, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析 catalog 事件", exception);
        }
    }

    private void rebuildSourceProjection(String sourceProductId,
                                         ObjectMapper objectMapper,
                                         SourceProductRepository sourceProductRepository,
                                         CatalogProjectionRepository catalogProjectionRepository) {
        SourceProductEntity sourceProduct = sourceProductRepository.findById(sourceProductId)
                .orElseThrow(() -> new IllegalArgumentException("源商品不存在: " + sourceProductId));
        ProductContentDocument content = readProductContent(objectMapper, sourceProduct.getProductContent());
        upsertProjection(
                objectMapper,
                catalogProjectionRepository,
                "MALL",
                sourceProduct.getSourceProductId(),
                sourceProduct.getSourceProductId(),
                null,
                sourceProduct.getProductViewId(),
                content,
                sourceProduct.getProductStatus(),
                sourceProduct.getSourceMode(),
                "SOURCE"
        );
        upsertProjection(
                objectMapper,
                catalogProjectionRepository,
                "DECORATION",
                sourceProduct.getSourceProductId(),
                sourceProduct.getSourceProductId(),
                null,
                sourceProduct.getProductViewId(),
                content,
                sourceProduct.getProductStatus(),
                sourceProduct.getSourceMode(),
                "SOURCE"
        );
    }

    private void rebuildMerchantOfferProjection(String merchantOfferProductId,
                                                ObjectMapper objectMapper,
                                                SourceProductRepository sourceProductRepository,
                                                MerchantOfferProductRepository merchantOfferProductRepository,
                                                CatalogProjectionRepository catalogProjectionRepository) {
        MerchantOfferProductEntity offerProduct = merchantOfferProductRepository.findById(merchantOfferProductId)
                .orElseThrow(() -> new IllegalArgumentException("经营商品不存在: " + merchantOfferProductId));
        SourceProductEntity sourceProduct = sourceProductRepository.findById(offerProduct.getSourceProductId())
                .orElseThrow(() -> new IllegalArgumentException("源商品不存在: " + offerProduct.getSourceProductId()));
        ProductContentDocument sourceContent = readProductContent(objectMapper, sourceProduct.getProductContent());
        ProductContentDocument offerContent = readProductContent(objectMapper, offerProduct.getOfferContent());
        ProductContentDocument mergedContent = sourceContent.mergeOverride(offerContent);
        upsertProjection(
                objectMapper,
                catalogProjectionRepository,
                "MALL",
                offerProduct.getMerchantOfferProductId(),
                sourceProduct.getSourceProductId(),
                offerProduct.getMerchantOfferProductId(),
                offerProduct.getProductViewId(),
                mergedContent,
                offerProduct.getOfferStatus(),
                sourceProduct.getSourceMode(),
                "MERCHANT_OFFER"
        );
        upsertProjection(
                objectMapper,
                catalogProjectionRepository,
                "DECORATION",
                offerProduct.getMerchantOfferProductId(),
                sourceProduct.getSourceProductId(),
                offerProduct.getMerchantOfferProductId(),
                offerProduct.getProductViewId(),
                mergedContent,
                offerProduct.getOfferStatus(),
                sourceProduct.getSourceMode(),
                "MERCHANT_OFFER"
        );
    }

    private void upsertProjection(ObjectMapper objectMapper,
                                  CatalogProjectionRepository catalogProjectionRepository,
                                  String presentationType,
                                  String businessProductId,
                                  String sourceProductId,
                                  String merchantOfferProductId,
                                  String productViewId,
                                  ProductContentDocument content,
                                  String productStatus,
                                  String sourceType,
                                  String productType) {
        String locale = content.defaultLocale() == null ? "zh-CN" : content.defaultLocale();
        LocalizedTextDocument.LocalizedTextResolution resolvedProductName =
                content.productName() == null
                        ? new LocalizedTextDocument.LocalizedTextResolution("", locale, true)
                        : content.productName().resolve(locale);
        CatalogProjectionPayload payload = new CatalogProjectionPayload(
                resolvedProductName.value(),
                content.coverImage(),
                productStatus,
                sourceType,
                resolvedProductName.resolvedLocale(),
                resolvedProductName.fallbackApplied(),
                content.coverImage() == null || content.coverImage().isBlank() ? "PARTIAL" : "COMPLETE",
                productType
        );
        String serializedPayload = writeProjectionPayload(objectMapper, payload);
        Optional<CatalogProjectionEntity> existing = catalogProjectionRepository
                .findByPresentationTypeAndProductViewIdAndLocale(presentationType, productViewId, locale);
        CatalogProjectionEntity projection = existing.orElseGet(() -> new CatalogProjectionEntity(
                UUID.randomUUID().toString(),
                presentationType,
                businessProductId,
                sourceProductId,
                merchantOfferProductId,
                productViewId,
                locale,
                serializedPayload,
                "ACTIVE",
                OffsetDateTime.now()
        ));
        projection.refresh(serializedPayload, "ACTIVE", OffsetDateTime.now());
        catalogProjectionRepository.save(projection);
    }

    private ProductContentDocument readProductContent(ObjectMapper objectMapper, String content) {
        try {
            if (content == null || content.isBlank()) {
                return new ProductContentDocument(null, null);
            }
            return objectMapper.readValue(content, ProductContentDocument.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析商品内容", exception);
        }
    }

    private String writeProjectionPayload(ObjectMapper objectMapper, CatalogProjectionPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化商品投影", exception);
        }
    }
}
