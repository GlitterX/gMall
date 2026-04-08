package com.gmall.catalog.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.catalog.infrastructure.messaging.CatalogEventAppender;
import com.gmall.catalog.infrastructure.messaging.CatalogEventPayload;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductEntity;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductRepository;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferSkuEntity;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferSkuRepository;
import com.gmall.catalog.infrastructure.persistence.SourceProductEntity;
import com.gmall.catalog.infrastructure.persistence.SourceProductRepository;
import com.gmall.catalog.infrastructure.persistence.SourceSkuEntity;
import com.gmall.catalog.infrastructure.persistence.SourceSkuRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogCommandService {

    private final SourceProductRepository sourceProductRepository;
    private final SourceSkuRepository sourceSkuRepository;
    private final MerchantOfferProductRepository merchantOfferProductRepository;
    private final MerchantOfferSkuRepository merchantOfferSkuRepository;
    private final FoundationSupplyRelationGateway foundationSupplyRelationGateway;
    private final CatalogEventAppender catalogEventAppender;
    private final ObjectMapper objectMapper;

    public CatalogCommandService(SourceProductRepository sourceProductRepository,
                                 SourceSkuRepository sourceSkuRepository,
                                 MerchantOfferProductRepository merchantOfferProductRepository,
                                 MerchantOfferSkuRepository merchantOfferSkuRepository,
                                 FoundationSupplyRelationGateway foundationSupplyRelationGateway,
                                 CatalogEventAppender catalogEventAppender,
                                 ObjectMapper objectMapper) {
        this.sourceProductRepository = sourceProductRepository;
        this.sourceSkuRepository = sourceSkuRepository;
        this.merchantOfferProductRepository = merchantOfferProductRepository;
        this.merchantOfferSkuRepository = merchantOfferSkuRepository;
        this.foundationSupplyRelationGateway = foundationSupplyRelationGateway;
        this.catalogEventAppender = catalogEventAppender;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CatalogQueryModels.SourceProductView createSourceProduct(CreateSourceProductCommand command) {
        validateSourceProductCommand(command);
        String sourceProductId = UUID.randomUUID().toString();
        String productViewId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        SourceProductEntity sourceProduct = new SourceProductEntity(
                sourceProductId,
                productViewId,
                command.ownerType(),
                command.ownerId(),
                command.sourceMode(),
                command.categoryId(),
                command.brandId(),
                writeJson(command.productContent()),
                "DRAFT",
                1L,
                now,
                now
        );
        sourceProductRepository.save(sourceProduct);
        for (SourceSkuInput sku : safeSkus(command.skus())) {
            sourceSkuRepository.save(new SourceSkuEntity(
                    UUID.randomUUID().toString(),
                    sourceProductId,
                    sku.specSignature(),
                    writeJson(sku.specPayload() == null ? Map.of() : sku.specPayload()),
                    "ACTIVE",
                    1L
            ));
        }
        catalogEventAppender.append(
                "SourceProduct",
                sourceProductId,
                "SourceProductCreated",
                1L,
                new CatalogEventPayload(sourceProductId, null, productViewId, null, null)
        );
        return new CatalogQueryModels.SourceProductView(
                sourceProductId,
                productViewId,
                command.ownerType(),
                command.ownerId(),
                command.sourceMode(),
                command.categoryId(),
                command.brandId(),
                "DRAFT",
                1L
        );
    }

    @Transactional
    public CatalogQueryModels.MerchantOfferProductView deriveMerchantOfferProduct(String sourceProductId,
                                                                                  DeriveMerchantOfferProductCommand command) {
        SourceProductEntity sourceProduct = sourceProductRepository.findById(sourceProductId)
                .orElseThrow(() -> new IllegalArgumentException("源商品不存在: " + sourceProductId));
        if (!"SUPPLY".equals(sourceProduct.getSourceMode())) {
            throw new IllegalArgumentException("仅供货源商品允许派生经营副本");
        }
        return merchantOfferProductRepository
                .findByMerchantIdAndRelationIdAndSourceProductId(command.merchantId(), command.relationId(), sourceProductId)
                .map(this::toOfferView)
                .orElseGet(() -> createOffer(sourceProduct, command));
    }

    @Transactional
    public CatalogQueryModels.SourceProductView updateSourceProduct(String sourceProductId,
                                                                    UpdateSourceProductCommand command) {
        validateUpdateSourceProductCommand(command);
        SourceProductEntity sourceProduct = sourceProductRepository.findById(sourceProductId)
                .orElseThrow(() -> new IllegalArgumentException("源商品不存在: " + sourceProductId));
        sourceProduct.update(
                writeJson(command.productContent()),
                command.productStatus(),
                OffsetDateTime.now()
        );
        sourceProductRepository.save(sourceProduct);
        catalogEventAppender.append(
                "SourceProduct",
                sourceProductId,
                "SourceProductUpdated",
                sourceProduct.getContentVersion(),
                new CatalogEventPayload(
                        sourceProductId,
                        null,
                        sourceProduct.getProductViewId(),
                        null,
                        null
                )
        );
        for (MerchantOfferProductEntity offerProduct : merchantOfferProductRepository.findBySourceProductId(sourceProductId)) {
            offerProduct.markInvalidPendingConfirm(OffsetDateTime.now());
            merchantOfferProductRepository.save(offerProduct);
            catalogEventAppender.append(
                    "MerchantOfferProduct",
                    offerProduct.getMerchantOfferProductId(),
                    "MerchantOfferProductUpdated",
                    1L,
                    new CatalogEventPayload(
                            sourceProductId,
                            offerProduct.getMerchantOfferProductId(),
                            offerProduct.getProductViewId(),
                            offerProduct.getMerchantId(),
                            offerProduct.getRelationId()
                    )
            );
        }
        return new CatalogQueryModels.SourceProductView(
                sourceProduct.getSourceProductId(),
                sourceProduct.getProductViewId(),
                sourceProduct.getOwnerType(),
                sourceProduct.getOwnerId(),
                sourceProduct.getSourceMode(),
                sourceProduct.getCategoryId(),
                sourceProduct.getBrandId(),
                sourceProduct.getProductStatus(),
                sourceProduct.getContentVersion()
        );
    }

    @Transactional
    public CatalogQueryModels.MerchantOfferProductView updateMerchantOfferProduct(String merchantOfferProductId,
                                                                                  UpdateMerchantOfferProductCommand command) {
        validateUpdateOfferProductCommand(command);
        MerchantOfferProductEntity offerProduct = merchantOfferProductRepository.findById(merchantOfferProductId)
                .orElseThrow(() -> new IllegalArgumentException("经营商品不存在: " + merchantOfferProductId));
        offerProduct.updateOfferContent(writeJson(command.offerContent()), command.offerStatus(), OffsetDateTime.now());
        merchantOfferProductRepository.save(offerProduct);
        catalogEventAppender.append(
                "MerchantOfferProduct",
                merchantOfferProductId,
                "MerchantOfferProductUpdated",
                1L,
                new CatalogEventPayload(
                        offerProduct.getSourceProductId(),
                        offerProduct.getMerchantOfferProductId(),
                        offerProduct.getProductViewId(),
                        offerProduct.getMerchantId(),
                        offerProduct.getRelationId()
                )
        );
        return toOfferView(offerProduct);
    }

    private CatalogQueryModels.MerchantOfferProductView createOffer(SourceProductEntity sourceProduct,
                                                                    DeriveMerchantOfferProductCommand command) {
        FoundationSupplyRelationGateway.AuthorizationDecision authorizationDecision =
                foundationSupplyRelationGateway.resolveSourceProductAuthorization(
                        sourceProduct.getOwnerId(),
                        command.merchantOrganizationId(),
                        sourceProduct.getSourceProductId()
                );
        if (!authorizationDecision.active() || !authorizationDecision.catalogAuthorized()) {
            throw new IllegalArgumentException("未授权派生经营商品: " + authorizationDecision.authorizationReason());
        }
        if (!command.relationId().equals(authorizationDecision.relationId())) {
            throw new IllegalArgumentException("供货关系不匹配: " + command.relationId());
        }

        String offerId = UUID.randomUUID().toString();
        String productViewId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        MerchantOfferProductEntity offerProduct = new MerchantOfferProductEntity(
                offerId,
                productViewId,
                command.merchantId(),
                command.relationId(),
                sourceProduct.getSourceProductId(),
                writeJson(command.offerContent() == null ? new ProductContentDocument(null, null) : command.offerContent()),
                "DRAFT",
                "SYNCED",
                now,
                now
        );
        merchantOfferProductRepository.save(offerProduct);
        List<SourceSkuEntity> sourceSkus = sourceSkuRepository.findBySourceProductId(sourceProduct.getSourceProductId());
        for (SourceSkuEntity sourceSku : sourceSkus) {
            merchantOfferSkuRepository.save(new MerchantOfferSkuEntity(
                    UUID.randomUUID().toString(),
                    offerId,
                    sourceSku.getSourceSkuId(),
                    "{}",
                    "ACTIVE",
                    1L
            ));
        }
        catalogEventAppender.append(
                "MerchantOfferProduct",
                offerId,
                "MerchantOfferProductCreated",
                1L,
                new CatalogEventPayload(
                        sourceProduct.getSourceProductId(),
                        offerId,
                        productViewId,
                        command.merchantId(),
                        command.relationId()
                )
        );
        return toOfferView(offerProduct);
    }

    private CatalogQueryModels.MerchantOfferProductView toOfferView(MerchantOfferProductEntity entity) {
        return new CatalogQueryModels.MerchantOfferProductView(
                entity.getMerchantOfferProductId(),
                entity.getProductViewId(),
                entity.getMerchantId(),
                entity.getRelationId(),
                entity.getSourceProductId(),
                entity.getOfferStatus()
        );
    }

    private void validateSourceProductCommand(CreateSourceProductCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("创建源商品命令不能为空");
        }
        if (!hasText(command.ownerType()) || !hasText(command.ownerId()) || !hasText(command.sourceMode())) {
            throw new IllegalArgumentException("源商品主体信息不完整");
        }
        if (!hasText(command.categoryId()) || command.productContent() == null || command.productContent().productName() == null) {
            throw new IllegalArgumentException("源商品内容不完整");
        }
    }

    private void validateUpdateSourceProductCommand(UpdateSourceProductCommand command) {
        if (command == null || command.productContent() == null || command.productContent().productName() == null) {
            throw new IllegalArgumentException("源商品更新内容不完整");
        }
        if (!hasText(command.productStatus())) {
            throw new IllegalArgumentException("源商品状态不能为空");
        }
    }

    private void validateUpdateOfferProductCommand(UpdateMerchantOfferProductCommand command) {
        if (command == null || command.offerContent() == null || command.offerContent().productName() == null) {
            throw new IllegalArgumentException("经营商品更新内容不完整");
        }
        if (!hasText(command.offerStatus())) {
            throw new IllegalArgumentException("经营商品状态不能为空");
        }
    }

    private List<SourceSkuInput> safeSkus(List<SourceSkuInput> skus) {
        return skus == null ? List.of() : skus;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化商品内容", exception);
        }
    }
}
