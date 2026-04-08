package com.gmall.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.catalog.infrastructure.persistence.CatalogProjectionEntity;
import com.gmall.catalog.infrastructure.persistence.CatalogProjectionRepository;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogQueryServiceTest {

    private final CatalogProjectionRepository catalogProjectionRepository = mock(CatalogProjectionRepository.class);
    private final MerchantOfferSkuRepository merchantOfferSkuRepository = mock(MerchantOfferSkuRepository.class);
    private final MerchantOfferProductRepository merchantOfferProductRepository = mock(MerchantOfferProductRepository.class);
    private final SourceProductRepository sourceProductRepository = mock(SourceProductRepository.class);
    private final SourceSkuRepository sourceSkuRepository = mock(SourceSkuRepository.class);

    private CatalogQueryService catalogQueryService;

    @BeforeEach
    void setUp() {
        catalogQueryService = new CatalogQueryService(
                catalogProjectionRepository,
                merchantOfferSkuRepository,
                merchantOfferProductRepository,
                sourceProductRepository,
                sourceSkuRepository,
                new ObjectMapper()
        );
    }

    @Test
    void getBusinessSkuMappingReturnsResolvedMerchantOfferSkuMapping() {
        when(merchantOfferSkuRepository.findById("offer-sku-1"))
                .thenReturn(Optional.of(new MerchantOfferSkuEntity(
                        "offer-sku-1",
                        "offer-product-1",
                        "source-sku-1",
                        "{\"zh-CN\":\"标准装\"}",
                        "ACTIVE",
                        1L
                )));
        when(merchantOfferProductRepository.findById("offer-product-1"))
                .thenReturn(Optional.of(new MerchantOfferProductEntity(
                        "offer-product-1",
                        "view-offer-1",
                        "merchant-1",
                        "relation-1",
                        "source-product-1",
                        "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"translations\":{\"zh-CN\":\"店铺礼盒\"}}}",
                        "ACTIVE",
                        "SYNCED",
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00")
                )));
        when(sourceProductRepository.findById("source-product-1"))
                .thenReturn(Optional.of(new SourceProductEntity(
                        "source-product-1",
                        "view-source-1",
                        "SUPPLIER",
                        "org-supplier",
                        "SUPPLY",
                        "cat-fruit",
                        "brand-organic",
                        "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"translations\":{\"zh-CN\":\"源商品\"}}}",
                        "ACTIVE",
                        1L,
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00")
                )));

        CatalogQueryModels.BusinessSkuMappingView view =
                catalogQueryService.getBusinessSkuMapping("MERCHANT_OFFER_SKU", "offer-sku-1");

        assertThat(view.businessSkuType()).isEqualTo("MERCHANT_OFFER_SKU");
        assertThat(view.businessSkuId()).isEqualTo("offer-sku-1");
        assertThat(view.sourceSkuId()).isEqualTo("source-sku-1");
        assertThat(view.sourceProductId()).isEqualTo("source-product-1");
        assertThat(view.merchantOfferProductId()).isEqualTo("offer-product-1");
        assertThat(view.relationId()).isEqualTo("relation-1");
        assertThat(view.merchantId()).isEqualTo("merchant-1");
        assertThat(view.ownerType()).isEqualTo("SUPPLIER");
        assertThat(view.ownerId()).isEqualTo("org-supplier");
        assertThat(view.sourceMode()).isEqualTo("SUPPLY");
        assertThat(view.sourceProductStatus()).isEqualTo("ACTIVE");
        assertThat(view.offerProductStatus()).isEqualTo("ACTIVE");
        assertThat(view.offerSkuStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getBusinessSkuMappingReturnsResolvedSourceSkuMappingForDirectSku() {
        when(sourceSkuRepository.findById("source-sku-direct-1"))
                .thenReturn(Optional.of(new SourceSkuEntity(
                        "source-sku-direct-1",
                        "source-product-direct-1",
                        "size:M",
                        "{\"size\":\"M\"}",
                        "ACTIVE",
                        2L
                )));
        when(sourceProductRepository.findById("source-product-direct-1"))
                .thenReturn(Optional.of(new SourceProductEntity(
                        "source-product-direct-1",
                        "view-source-direct-1",
                        "SUPPLIER",
                        "org-direct-1",
                        "DIRECT",
                        "cat-fashion",
                        "brand-1",
                        "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"translations\":{\"zh-CN\":\"直营外套\"}}}",
                        "ON_SHELF",
                        3L,
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00")
                )));

        CatalogQueryModels.BusinessSkuMappingView view =
                catalogQueryService.getBusinessSkuMapping("SOURCE_SKU", "source-sku-direct-1");

        assertThat(view.businessSkuType()).isEqualTo("SOURCE_SKU");
        assertThat(view.businessSkuId()).isEqualTo("source-sku-direct-1");
        assertThat(view.sourceSkuId()).isEqualTo("source-sku-direct-1");
        assertThat(view.sourceProductId()).isEqualTo("source-product-direct-1");
        assertThat(view.ownerId()).isEqualTo("org-direct-1");
        assertThat(view.sourceMode()).isEqualTo("DIRECT");
        assertThat(view.merchantId()).isNull();
        assertThat(view.offerProductStatus()).isNull();
        assertThat(view.offerSkuStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getBusinessSkuMappingThrowsWhenOfferSkuIsMissing() {
        when(merchantOfferSkuRepository.findById("offer-sku-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogQueryService.getBusinessSkuMapping("MERCHANT_OFFER_SKU", "offer-sku-missing"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("offer-sku-missing");
    }

    @Test
    void getSourceProductManagementViewReturnsSourceProductAndSkus() {
        when(sourceProductRepository.findById("source-product-2"))
                .thenReturn(Optional.of(new SourceProductEntity(
                        "source-product-2",
                        "view-source-2",
                        "SUPPLIER",
                        "org-supplier-2",
                        "SUPPLY",
                        "cat-fruit",
                        "brand-2",
                        "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"translations\":{\"zh-CN\":\"源商品2\"}}}",
                        "ACTIVE",
                        5L,
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-07T18:00:00+08:00")
                )));
        when(sourceSkuRepository.findBySourceProductId("source-product-2"))
                .thenReturn(List.of(new SourceSkuEntity(
                        "source-sku-2",
                        "source-product-2",
                        "weight:500g",
                        "{\"weight\":\"500g\"}",
                        "ACTIVE",
                        3L
                )));

        CatalogQueryModels.SourceProductManagementView view = catalogQueryService.getSourceProductManagement("source-product-2");

        assertThat(view.sourceProductId()).isEqualTo("source-product-2");
        assertThat(view.ownerId()).isEqualTo("org-supplier-2");
        assertThat(view.skus()).singleElement().satisfies(sku -> {
            assertThat(sku.sourceSkuId()).isEqualTo("source-sku-2");
            assertThat(sku.skuStatus()).isEqualTo("ACTIVE");
        });
    }

    @Test
    void getMerchantOfferProductManagementViewReturnsOfferProductAndSkus() {
        when(merchantOfferProductRepository.findById("offer-product-2"))
                .thenReturn(Optional.of(new MerchantOfferProductEntity(
                        "offer-product-2",
                        "view-offer-2",
                        "merchant-2",
                        "relation-2",
                        "source-product-2",
                        "{\"productName\":{\"defaultLocale\":\"zh-CN\",\"translations\":{\"zh-CN\":\"经营商品2\"}}}",
                        "INVALID_PENDING_CONFIRM",
                        "PENDING_CONFIRM",
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-07T18:00:00+08:00")
                )));
        when(merchantOfferSkuRepository.findByMerchantOfferProductId("offer-product-2"))
                .thenReturn(List.of(new MerchantOfferSkuEntity(
                        "offer-sku-2",
                        "offer-product-2",
                        "source-sku-2",
                        "{\"zh-CN\":\"经营规格\"}",
                        "ACTIVE",
                        4L
                )));

        CatalogQueryModels.MerchantOfferProductManagementView view =
                catalogQueryService.getMerchantOfferProductManagement("offer-product-2");

        assertThat(view.merchantOfferProductId()).isEqualTo("offer-product-2");
        assertThat(view.offerStatus()).isEqualTo("INVALID_PENDING_CONFIRM");
        assertThat(view.syncConfirmationStatus()).isEqualTo("PENDING_CONFIRM");
        assertThat(view.skus()).singleElement().satisfies(sku -> {
            assertThat(sku.merchantOfferSkuId()).isEqualTo("offer-sku-2");
            assertThat(sku.sourceSkuId()).isEqualTo("source-sku-2");
            assertThat(sku.offerSkuStatus()).isEqualTo("ACTIVE");
        });
    }

    @Test
    void getProductFallsBackToLatestProjectionWhenLocaleSpecificProjectionMissing() {
        when(catalogProjectionRepository.findByPresentationTypeAndProductViewIdAndLocale("MALL", "view-source-2", "en-US"))
                .thenReturn(Optional.empty());
        when(catalogProjectionRepository.findByPresentationTypeAndProductViewIdOrderByUpdatedAtDesc("MALL", "view-source-2"))
                .thenReturn(List.of(new CatalogProjectionEntity(
                        "projection-1",
                        "MALL",
                        "source-2",
                        "source-2",
                        null,
                        "view-source-2",
                        "zh-CN",
                        "{\"title\":\"有机蓝莓礼盒\",\"coverImage\":\"https://img.example.com/source.png\",\"productStatus\":\"ACTIVE\",\"sourceType\":\"SUPPLY\",\"resolvedLocale\":\"zh-CN\",\"fallbackApplied\":true,\"contentCompleteness\":\"COMPLETE\",\"productType\":\"SOURCE\"}",
                        "ACTIVE",
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00")
                )));

        CatalogQueryModels.ProductDetailView view = catalogQueryService.getProduct("view-source-2", "en-US");

        assertThat(view.productViewId()).isEqualTo("view-source-2");
        assertThat(view.resolvedLocale()).isEqualTo("zh-CN");
        assertThat(view.fallbackApplied()).isTrue();
        assertThat(view.sourceType()).isEqualTo("SUPPLY");
        assertThat(view.contentCompleteness()).isEqualTo("COMPLETE");
    }

    @Test
    void getDecorationProductsFallsBackToLatestProjectionWhenLocaleSpecificProjectionMissing() {
        when(catalogProjectionRepository.findByPresentationTypeAndProductViewIdAndLocale("DECORATION", "view-offer-3", "en-US"))
                .thenReturn(Optional.empty());
        when(catalogProjectionRepository.findByPresentationTypeAndProductViewIdOrderByUpdatedAtDesc("DECORATION", "view-offer-3"))
                .thenReturn(List.of(new CatalogProjectionEntity(
                        "projection-9",
                        "DECORATION",
                        "offer-3",
                        "source-3",
                        "offer-3",
                        "view-offer-3",
                        "zh-CN",
                        "{\"title\":\"店铺精选礼盒\",\"coverImage\":\"https://img.example.com/offer.png\",\"productStatus\":\"ACTIVE\",\"sourceType\":\"SUPPLY\",\"resolvedLocale\":\"zh-CN\",\"fallbackApplied\":true,\"contentCompleteness\":\"COMPLETE\",\"productType\":\"MERCHANT_OFFER\"}",
                        "ACTIVE",
                        OffsetDateTime.parse("2026-04-06T18:00:00+08:00")
                )));

        List<CatalogQueryModels.DecorationProductView> views =
                catalogQueryService.getDecorationProducts(List.of("view-offer-3"), "en-US");

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.productViewId()).isEqualTo("view-offer-3");
            assertThat(view.resolvedLocale()).isEqualTo("zh-CN");
            assertThat(view.fallbackApplied()).isTrue();
            assertThat(view.contentCompleteness()).isEqualTo("COMPLETE");
        });
    }

    @Test
    void listSourceProductManagementSupportsPaginationAndSorting() {
        when(sourceProductRepository.findByOwnerId("org-supplier-3"))
                .thenReturn(List.of(
                        new SourceProductEntity(
                                "source-3",
                                "view-source-3",
                                "SUPPLIER",
                                "org-supplier-3",
                                "SUPPLY",
                                "cat-3",
                                "brand-3",
                                "{}",
                                "ACTIVE",
                                2L,
                                OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-07T18:00:00+08:00")
                        ),
                        new SourceProductEntity(
                                "source-5",
                                "view-source-5",
                                "SUPPLIER",
                                "org-supplier-3",
                                "SUPPLY",
                                "cat-5",
                                "brand-5",
                                "{}",
                                "ACTIVE",
                                5L,
                                OffsetDateTime.parse("2026-04-06T19:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-07T19:00:00+08:00")
                        ),
                        new SourceProductEntity(
                                "source-4",
                                "view-source-4",
                                "SUPPLIER",
                                "org-supplier-3",
                                "DIRECT",
                                "cat-4",
                                "brand-4",
                                "{}",
                                "DRAFT",
                                1L,
                                OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-07T17:00:00+08:00")
                        )
                ));

        CatalogQueryModels.PageResult<CatalogQueryModels.SourceProductSummaryView> page =
                catalogQueryService.listSourceProductManagement(
                        "org-supplier-3",
                        "SUPPLIER",
                        "SUPPLY",
                        "ACTIVE",
                        1,
                        1,
                        "contentVersion",
                        "ASC"
                );

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.items()).singleElement().satisfies(view -> {
            assertThat(view.sourceProductId()).isEqualTo("source-3");
            assertThat(view.sourceMode()).isEqualTo("SUPPLY");
            assertThat(view.productStatus()).isEqualTo("ACTIVE");
        });
    }

    @Test
    void listMerchantOfferProductManagementSupportsPaginationAndSorting() {
        when(merchantOfferProductRepository.findByMerchantId("merchant-3"))
                .thenReturn(List.of(
                        new MerchantOfferProductEntity(
                                "offer-3",
                                "view-offer-3",
                                "merchant-3",
                                "relation-3",
                                "source-3",
                                "{}",
                                "ACTIVE",
                                "SYNCED",
                                OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-07T18:00:00+08:00")
                        ),
                        new MerchantOfferProductEntity(
                                "offer-5",
                                "view-offer-5",
                                "merchant-3",
                                "relation-3",
                                "source-5",
                                "{}",
                                "ACTIVE",
                                "SYNCED",
                                OffsetDateTime.parse("2026-04-06T16:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-07T16:00:00+08:00")
                        ),
                        new MerchantOfferProductEntity(
                                "offer-4",
                                "view-offer-4",
                                "merchant-3",
                                "relation-4",
                                "source-4",
                                "{}",
                                "INVALID_PENDING_CONFIRM",
                                "PENDING_CONFIRM",
                                OffsetDateTime.parse("2026-04-06T18:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-07T17:00:00+08:00")
                        )
                ));

        CatalogQueryModels.PageResult<CatalogQueryModels.MerchantOfferProductSummaryView> page =
                catalogQueryService.listMerchantOfferProductManagement(
                        "merchant-3",
                        "relation-3",
                        "ACTIVE",
                        1,
                        1,
                        "updatedAt",
                        "ASC"
                );

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.items()).singleElement().satisfies(view -> {
            assertThat(view.merchantOfferProductId()).isEqualTo("offer-5");
            assertThat(view.relationId()).isEqualTo("relation-3");
            assertThat(view.offerStatus()).isEqualTo("ACTIVE");
        });
    }
}
