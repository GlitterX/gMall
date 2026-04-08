package com.gmall.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.catalog.infrastructure.messaging.CatalogEventAppender;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductEntity;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductRepository;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferSkuEntity;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferSkuRepository;
import com.gmall.catalog.infrastructure.persistence.SourceProductEntity;
import com.gmall.catalog.infrastructure.persistence.SourceProductRepository;
import com.gmall.catalog.infrastructure.persistence.SourceSkuEntity;
import com.gmall.catalog.infrastructure.persistence.SourceSkuRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogCommandServiceTest {

    private final SourceProductRepository sourceProductRepository = mock(SourceProductRepository.class);
    private final SourceSkuRepository sourceSkuRepository = mock(SourceSkuRepository.class);
    private final MerchantOfferProductRepository merchantOfferProductRepository = mock(MerchantOfferProductRepository.class);
    private final MerchantOfferSkuRepository merchantOfferSkuRepository = mock(MerchantOfferSkuRepository.class);
    private final FoundationSupplyRelationGateway foundationSupplyRelationGateway = mock(FoundationSupplyRelationGateway.class);
    private final CatalogEventAppender catalogEventAppender = mock(CatalogEventAppender.class);

    private final List<SourceProductEntity> savedSourceProducts = new ArrayList<>();
    private final List<SourceSkuEntity> savedSourceSkus = new ArrayList<>();
    private final List<MerchantOfferProductEntity> savedOfferProducts = new ArrayList<>();
    private final List<MerchantOfferSkuEntity> savedOfferSkus = new ArrayList<>();

    private CatalogCommandService catalogCommandService;

    @BeforeEach
    void setUp() {
        catalogCommandService = new CatalogCommandService(
                sourceProductRepository,
                sourceSkuRepository,
                merchantOfferProductRepository,
                merchantOfferSkuRepository,
                foundationSupplyRelationGateway,
                catalogEventAppender,
                new ObjectMapper()
        );
        when(sourceProductRepository.save(any(SourceProductEntity.class))).thenAnswer(invocation -> {
            SourceProductEntity entity = invocation.getArgument(0);
            savedSourceProducts.add(entity);
            return entity;
        });
        when(sourceSkuRepository.save(any(SourceSkuEntity.class))).thenAnswer(invocation -> {
            SourceSkuEntity entity = invocation.getArgument(0);
            savedSourceSkus.add(entity);
            return entity;
        });
        when(merchantOfferProductRepository.save(any(MerchantOfferProductEntity.class))).thenAnswer(invocation -> {
            MerchantOfferProductEntity entity = invocation.getArgument(0);
            savedOfferProducts.add(entity);
            return entity;
        });
        when(merchantOfferSkuRepository.save(any(MerchantOfferSkuEntity.class))).thenAnswer(invocation -> {
            MerchantOfferSkuEntity entity = invocation.getArgument(0);
            savedOfferSkus.add(entity);
            return entity;
        });
    }

    @Test
    void createSourceProductAppendsOutboxEvent() {
        CatalogQueryModels.SourceProductView view = catalogCommandService.createSourceProduct(
                new CreateSourceProductCommand(
                        "SUPPLIER",
                        "org-supplier",
                        "SUPPLY",
                        "cat-fruit",
                        "brand-organic",
                        new ProductContentDocument(
                                new LocalizedTextDocument(
                                        "zh-CN",
                                        "DEFAULT_LOCALE",
                                        Map.of(
                                                "zh-CN", "有机草莓礼盒",
                                                "en-US", "Organic Strawberry Gift Box"
                                        )
                                ),
                                "https://img.example.com/strawberry.png"
                        ),
                        List.of(new SourceSkuInput("weight:1kg", Map.of("weight", "1kg")))
                )
        );

        assertThat(view.sourceProductId()).isNotBlank();
        assertThat(view.productViewId()).isNotBlank();
        assertThat(savedSourceProducts).hasSize(1);
        assertThat(savedSourceSkus).hasSize(1);
        verify(catalogEventAppender).append(
                eq("SourceProduct"),
                eq(view.sourceProductId()),
                eq("SourceProductCreated"),
                eq(1L),
                any()
        );
    }

    @Test
    void deriveMerchantOfferProductCopiesSourceMappingAfterAuthorization() {
        SourceProductEntity sourceProduct = new SourceProductEntity(
                "source-1",
                "view-source-1",
                "SUPPLIER",
                "org-supplier",
                "SUPPLY",
                "cat-fruit",
                "brand-organic",
                "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"fallbackPolicy\":\"DEFAULT_LOCALE\",\"translations\":{\"zh-CN\":\"有机草莓礼盒\"}},\"coverImage\":\"https://img.example.com/source.png\"}",
                "DRAFT",
                1L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        );
        when(sourceProductRepository.findById("source-1")).thenReturn(Optional.of(sourceProduct));
        when(sourceSkuRepository.findBySourceProductId("source-1")).thenReturn(List.of(
                new SourceSkuEntity(
                        "source-sku-1",
                        "source-1",
                        "weight:1kg",
                        "{\"weight\":\"1kg\"}",
                        "ACTIVE",
                        1L
                )
        ));
        when(foundationSupplyRelationGateway.resolveSourceProductAuthorization("org-supplier", "org-merchant", "source-1"))
                .thenReturn(new FoundationSupplyRelationGateway.AuthorizationDecision(
                        "rel-1",
                        true,
                        true,
                        "PRODUCT_AUTHORIZED"
                ));

        CatalogQueryModels.MerchantOfferProductView view = catalogCommandService.deriveMerchantOfferProduct(
                "source-1",
                new DeriveMerchantOfferProductCommand(
                        "merchant-1",
                        "org-merchant",
                        "rel-1",
                        new ProductContentDocument(
                                new LocalizedTextDocument(
                                        "zh-CN",
                                        "DEFAULT_LOCALE",
                                        Map.of("zh-CN", "店铺精选有机草莓礼盒")
                                ),
                                "https://img.example.com/offer.png"
                        )
                )
        );

        assertThat(view.merchantOfferProductId()).isNotBlank();
        assertThat(savedOfferProducts).singleElement().satisfies(entity -> {
            assertThat(entity.getMerchantId()).isEqualTo("merchant-1");
            assertThat(entity.getRelationId()).isEqualTo("rel-1");
            assertThat(entity.getSourceProductId()).isEqualTo("source-1");
        });
        assertThat(savedOfferSkus).singleElement().satisfies(entity -> {
            assertThat(entity.getSourceSkuId()).isEqualTo("source-sku-1");
            assertThat(entity.getMappingVersion()).isEqualTo(1L);
        });
        verify(catalogEventAppender).append(
                eq("MerchantOfferProduct"),
                eq(view.merchantOfferProductId()),
                eq("MerchantOfferProductCreated"),
                eq(1L),
                any()
        );
    }

    @Test
    void deriveMerchantOfferProductRejectsUnauthorizedRelation() {
        SourceProductEntity sourceProduct = new SourceProductEntity(
                "source-2",
                "view-source-2",
                "SUPPLIER",
                "org-supplier",
                "SUPPLY",
                "cat-fruit",
                null,
                "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"fallbackPolicy\":\"DEFAULT_LOCALE\",\"translations\":{\"zh-CN\":\"有机蓝莓礼盒\"}},\"coverImage\":null}",
                "DRAFT",
                1L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        );
        when(sourceProductRepository.findById("source-2")).thenReturn(Optional.of(sourceProduct));
        when(foundationSupplyRelationGateway.resolveSourceProductAuthorization("org-supplier", "org-merchant", "source-2"))
                .thenReturn(new FoundationSupplyRelationGateway.AuthorizationDecision(
                        "rel-2",
                        true,
                        false,
                        "PRODUCT_SCOPE_REJECTED"
                ));

        assertThatThrownBy(() -> catalogCommandService.deriveMerchantOfferProduct(
                "source-2",
                new DeriveMerchantOfferProductCommand(
                        "merchant-1",
                        "org-merchant",
                        "rel-2",
                        new ProductContentDocument(null, null)
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未授权");
    }

    @Test
    void updateSourceProductInvalidatesDerivedOffersAndAppendsEvents() {
        SourceProductEntity sourceProduct = new SourceProductEntity(
                "source-3",
                "view-source-3",
                "SUPPLIER",
                "org-supplier",
                "SUPPLY",
                "cat-fruit",
                "brand-organic",
                "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"translations\":{\"zh-CN\":\"旧标题\"}},\"coverImage\":\"https://img.example.com/old-source.png\"}",
                "ACTIVE",
                2L,
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        );
        MerchantOfferProductEntity offerProduct = new MerchantOfferProductEntity(
                "offer-3",
                "view-offer-3",
                "merchant-3",
                "rel-3",
                "source-3",
                "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"translations\":{\"zh-CN\":\"商家标题\"}},\"coverImage\":\"https://img.example.com/offer.png\"}",
                "ACTIVE",
                "SYNCED",
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        );
        when(sourceProductRepository.findById("source-3")).thenReturn(Optional.of(sourceProduct));
        when(merchantOfferProductRepository.findBySourceProductId("source-3")).thenReturn(List.of(offerProduct));

        CatalogQueryModels.SourceProductView view = catalogCommandService.updateSourceProduct(
                "source-3",
                new UpdateSourceProductCommand(
                        new ProductContentDocument(
                                new LocalizedTextDocument(
                                        "zh-CN",
                                        "DEFAULT_LOCALE",
                                        Map.of("zh-CN", "新标题")
                                ),
                                "https://img.example.com/new-source.png"
                        ),
                        "ON_SHELF"
                )
        );

        assertThat(view.sourceProductId()).isEqualTo("source-3");
        assertThat(view.productStatus()).isEqualTo("ON_SHELF");
        assertThat(view.contentVersion()).isEqualTo(3L);
        assertThat(savedOfferProducts).singleElement().satisfies(saved -> {
            assertThat(saved.getOfferStatus()).isEqualTo("INVALID_PENDING_CONFIRM");
            assertThat(saved.getSyncConfirmationStatus()).isEqualTo("PENDING_CONFIRM");
        });
        verify(catalogEventAppender).append(eq("SourceProduct"), eq("source-3"), eq("SourceProductUpdated"), eq(3L), any());
        verify(catalogEventAppender).append(eq("MerchantOfferProduct"), eq("offer-3"), eq("MerchantOfferProductUpdated"), eq(1L), any());
    }

    @Test
    void updateMerchantOfferProductUpdatesContentAndAppendsEvent() {
        MerchantOfferProductEntity offerProduct = new MerchantOfferProductEntity(
                "offer-4",
                "view-offer-4",
                "merchant-4",
                "rel-4",
                "source-4",
                "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"translations\":{\"zh-CN\":\"旧经营标题\"}},\"coverImage\":\"https://img.example.com/old-offer.png\"}",
                "INVALID_PENDING_CONFIRM",
                "PENDING_CONFIRM",
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00+08:00")
        );
        when(merchantOfferProductRepository.findById("offer-4")).thenReturn(Optional.of(offerProduct));

        CatalogQueryModels.MerchantOfferProductView view = catalogCommandService.updateMerchantOfferProduct(
                "offer-4",
                new UpdateMerchantOfferProductCommand(
                        new ProductContentDocument(
                                new LocalizedTextDocument(
                                        "zh-CN",
                                        "DEFAULT_LOCALE",
                                        Map.of("zh-CN", "新经营标题")
                                ),
                                "https://img.example.com/new-offer.png"
                        ),
                        "ACTIVE"
                )
        );

        assertThat(view.merchantOfferProductId()).isEqualTo("offer-4");
        assertThat(view.offerStatus()).isEqualTo("ACTIVE");
        assertThat(savedOfferProducts).singleElement().satisfies(saved -> {
            assertThat(saved.getOfferStatus()).isEqualTo("ACTIVE");
            assertThat(saved.getSyncConfirmationStatus()).isEqualTo("SYNCED");
            assertThat(saved.getOfferContent()).contains("新经营标题");
        });
        verify(catalogEventAppender).append(eq("MerchantOfferProduct"), eq("offer-4"), eq("MerchantOfferProductUpdated"), eq(1L), any());
    }
}
