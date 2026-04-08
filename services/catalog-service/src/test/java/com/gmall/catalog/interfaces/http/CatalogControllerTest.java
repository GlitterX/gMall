package com.gmall.catalog.interfaces.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmall.catalog.application.CatalogCommandService;
import com.gmall.catalog.application.CatalogQueryModels;
import com.gmall.catalog.application.CatalogQueryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CatalogControllerTest {

    private final CatalogCommandService catalogCommandService = mock(CatalogCommandService.class);
    private final CatalogQueryService catalogQueryService = mock(CatalogQueryService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CatalogController controller = new CatalogController(catalogCommandService, catalogQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CatalogHttpExceptionHandler())
                .build();
    }

    @Test
    void createSourceProductReturnsCreatedView() throws Exception {
        when(catalogCommandService.createSourceProduct(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CatalogQueryModels.SourceProductView(
                        "source-1",
                        "view-source-1",
                        "SUPPLIER",
                        "org-supplier",
                        "SUPPLY",
                        "cat-fruit",
                        "brand-organic",
                        "DRAFT",
                        1L
                ));

        mockMvc.perform(post("/api/catalog/admin/source-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerType": "SUPPLIER",
                                  "ownerId": "org-supplier",
                                  "sourceMode": "SUPPLY",
                                  "categoryId": "cat-fruit",
                                  "brandId": "brand-organic",
                                  "productContent": {
                                    "productName": {
                                      "defaultLocale": "zh-CN",
                                      "fallbackPolicy": "DEFAULT_LOCALE",
                                      "translations": {
                                        "zh-CN": "有机草莓礼盒"
                                      }
                                    },
                                    "coverImage": "https://img.example.com/source.png"
                                  },
                                  "skus": [
                                    {
                                      "specSignature": "weight:1kg",
                                      "specPayload": {
                                        "weight": "1kg"
                                      }
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceProductId").value("source-1"))
                .andExpect(jsonPath("$.productViewId").value("view-source-1"));
    }

    @Test
    void getProductReturnsProjectionView() throws Exception {
        when(catalogQueryService.getProduct("view-source-1", "zh-CN"))
                .thenReturn(new CatalogQueryModels.ProductDetailView(
                        "view-source-1",
                        "source-1",
                        "source-1",
                        null,
                        "SOURCE",
                        "有机草莓礼盒",
                        "https://img.example.com/source.png",
                        "ACTIVE",
                        "SUPPLY",
                        "zh-CN",
                        false,
                        "COMPLETE"
                ));

        mockMvc.perform(get("/api/catalog/internal/products/view-source-1")
                        .param("locale", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productViewId").value("view-source-1"))
                .andExpect(jsonPath("$.title").value("有机草莓礼盒"))
                .andExpect(jsonPath("$.sourceType").value("SUPPLY"))
                .andExpect(jsonPath("$.contentCompleteness").value("COMPLETE"));
    }

    @Test
    void getDecorationProjectionReturnsProjectionCards() throws Exception {
        when(catalogQueryService.getDecorationProducts(List.of("view-source-1"), "zh-CN"))
                .thenReturn(List.of(new CatalogQueryModels.DecorationProductView(
                        "view-source-1",
                        "有机草莓礼盒",
                        "https://img.example.com/source.png",
                        "ACTIVE",
                        "SUPPLY",
                        "zh-CN",
                        false,
                        "COMPLETE"
                )));

        mockMvc.perform(post("/api/catalog/internal/projections/decoration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productViewIds": ["view-source-1"],
                                  "locale": "zh-CN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productViewId").value("view-source-1"))
                .andExpect(jsonPath("$[0].contentCompleteness").value("COMPLETE"));
    }

    @Test
    void getOfferSkuMappingReturnsResolvedSourceSkuMapping() throws Exception {
        when(catalogQueryService.getBusinessSkuMapping("MERCHANT_OFFER_SKU", "offer-sku-1"))
                .thenReturn(new CatalogQueryModels.BusinessSkuMappingView(
                        "MERCHANT_OFFER_SKU",
                        "offer-sku-1",
                        "offer-product-1",
                        "source-product-1",
                        "source-sku-1",
                        "SUPPLIER",
                        "org-supplier",
                        "SUPPLY",
                        "relation-1",
                        "merchant-1",
                        "ACTIVE",
                        "ACTIVE",
                        "ACTIVE"
                ));

        mockMvc.perform(get("/api/catalog/internal/skus/MERCHANT_OFFER_SKU/offer-sku-1/mapping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessSkuType").value("MERCHANT_OFFER_SKU"))
                .andExpect(jsonPath("$.businessSkuId").value("offer-sku-1"))
                .andExpect(jsonPath("$.sourceSkuId").value("source-sku-1"))
                .andExpect(jsonPath("$.merchantId").value("merchant-1"))
                .andExpect(jsonPath("$.ownerType").value("SUPPLIER"));
    }

    @Test
    void getSourceProductManagementReturnsProductAndSkus() throws Exception {
        when(catalogQueryService.getSourceProductManagement("source-9"))
                .thenReturn(new CatalogQueryModels.SourceProductManagementView(
                        "source-9",
                        "view-source-9",
                        "SUPPLIER",
                        "org-supplier-9",
                        "SUPPLY",
                        "cat-9",
                        "brand-9",
                        "ACTIVE",
                        6L,
                        List.of(new CatalogQueryModels.SourceSkuView("source-sku-9", "ACTIVE", 2L))
                ));

        mockMvc.perform(get("/api/catalog/admin/source-products/source-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceProductId").value("source-9"))
                .andExpect(jsonPath("$.skus[0].sourceSkuId").value("source-sku-9"));
    }

    @Test
    void listSourceProductManagementReturnsSummaries() throws Exception {
        when(catalogQueryService.listSourceProductManagement("org-supplier-9", "SUPPLIER", "SUPPLY", "ACTIVE", 1, 20, "updatedAt", "DESC"))
                .thenReturn(new CatalogQueryModels.PageResult<>(
                        1,
                        20,
                        1,
                        1,
                        false,
                        "updatedAt",
                        "DESC",
                        List.of(new CatalogQueryModels.SourceProductSummaryView(
                                "source-9",
                                "view-source-9",
                                "SUPPLIER",
                                "org-supplier-9",
                                "SUPPLY",
                                "ACTIVE",
                                6L
                        ))
                ));

        mockMvc.perform(get("/api/catalog/admin/source-products")
                        .param("ownerId", "org-supplier-9")
                        .param("ownerType", "SUPPLIER")
                        .param("sourceMode", "SUPPLY")
                        .param("productStatus", "ACTIVE")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .param("sortBy", "updatedAt")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pageNo").value(1))
                .andExpect(jsonPath("$.items[0].sourceProductId").value("source-9"))
                .andExpect(jsonPath("$.items[0].productStatus").value("ACTIVE"));
    }

    @Test
    void getMerchantOfferProductManagementReturnsProductAndSkus() throws Exception {
        when(catalogQueryService.getMerchantOfferProductManagement("offer-9"))
                .thenReturn(new CatalogQueryModels.MerchantOfferProductManagementView(
                        "offer-9",
                        "view-offer-9",
                        "merchant-9",
                        "relation-9",
                        "source-9",
                        "INVALID_PENDING_CONFIRM",
                        "PENDING_CONFIRM",
                        List.of(new CatalogQueryModels.MerchantOfferSkuView(
                                "offer-sku-9",
                                "source-sku-9",
                                "ACTIVE",
                                3L
                        ))
                ));

        mockMvc.perform(get("/api/catalog/merchant/offer-products/offer-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantOfferProductId").value("offer-9"))
                .andExpect(jsonPath("$.syncConfirmationStatus").value("PENDING_CONFIRM"))
                .andExpect(jsonPath("$.skus[0].merchantOfferSkuId").value("offer-sku-9"));
    }

    @Test
    void listMerchantOfferProductManagementReturnsSummaries() throws Exception {
        when(catalogQueryService.listMerchantOfferProductManagement("merchant-9", "relation-9", "ACTIVE", 1, 20, "updatedAt", "DESC"))
                .thenReturn(new CatalogQueryModels.PageResult<>(
                        1,
                        20,
                        1,
                        1,
                        false,
                        "updatedAt",
                        "DESC",
                        List.of(new CatalogQueryModels.MerchantOfferProductSummaryView(
                                "offer-9",
                                "view-offer-9",
                                "merchant-9",
                                "relation-9",
                                "source-9",
                                "ACTIVE",
                                "SYNCED"
                        ))
                ));

        mockMvc.perform(get("/api/catalog/merchant/offer-products")
                        .param("merchantId", "merchant-9")
                        .param("relationId", "relation-9")
                        .param("offerStatus", "ACTIVE")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .param("sortBy", "updatedAt")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pageNo").value(1))
                .andExpect(jsonPath("$.items[0].merchantOfferProductId").value("offer-9"))
                .andExpect(jsonPath("$.items[0].syncConfirmationStatus").value("SYNCED"));
    }

    @Test
    void updateSourceProductReturnsUpdatedView() throws Exception {
        when(catalogCommandService.updateSourceProduct(org.mockito.ArgumentMatchers.eq("source-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CatalogQueryModels.SourceProductView(
                        "source-1",
                        "view-source-1",
                        "SUPPLIER",
                        "org-supplier",
                        "SUPPLY",
                        "cat-fruit",
                        "brand-organic",
                        "ON_SHELF",
                        2L
                ));

        mockMvc.perform(put("/api/catalog/admin/source-products/source-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productContent": {
                                    "productName": {
                                      "defaultLocale": "zh-CN",
                                      "fallbackPolicy": "DEFAULT_LOCALE",
                                      "translations": {
                                        "zh-CN": "新源商品标题"
                                      }
                                    },
                                    "coverImage": "https://img.example.com/new-source.png"
                                  },
                                  "productStatus": "ON_SHELF"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productStatus").value("ON_SHELF"))
                .andExpect(jsonPath("$.contentVersion").value(2));
    }

    @Test
    void updateMerchantOfferProductReturnsUpdatedView() throws Exception {
        when(catalogCommandService.updateMerchantOfferProduct(org.mockito.ArgumentMatchers.eq("offer-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CatalogQueryModels.MerchantOfferProductView(
                        "offer-1",
                        "view-offer-1",
                        "merchant-1",
                        "relation-1",
                        "source-1",
                        "ACTIVE"
                ));

        mockMvc.perform(patch("/api/catalog/merchant/offer-products/offer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "offerContent": {
                                    "productName": {
                                      "defaultLocale": "zh-CN",
                                      "fallbackPolicy": "DEFAULT_LOCALE",
                                      "translations": {
                                        "zh-CN": "新经营商品标题"
                                      }
                                    },
                                    "coverImage": "https://img.example.com/new-offer.png"
                                  },
                                  "offerStatus": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantOfferProductId").value("offer-1"))
                .andExpect(jsonPath("$.offerStatus").value("ACTIVE"));
    }
}
