package com.gmall.trade.interfaces.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmall.trade.application.OrderRejectedException;
import com.gmall.trade.application.PricingInventoryGateway;
import com.gmall.trade.application.SubmitOrderSagaService;
import com.gmall.trade.application.TradeCartService;
import com.gmall.trade.application.TradeQueryService;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionEntity;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionRepository;
import com.gmall.trade.infrastructure.persistence.OrderRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TradeControllerTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final EligibilityProjectionRepository eligibilityProjectionRepository = mock(EligibilityProjectionRepository.class);
    private final PricingInventoryGateway pricingInventoryGateway = mock(PricingInventoryGateway.class);
    private final SubmitOrderSagaService submitOrderSagaService = mock(SubmitOrderSagaService.class);
    private final TradeCartService tradeCartService = mock(TradeCartService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TradeQueryService tradeQueryService = new TradeQueryService(orderRepository, eligibilityProjectionRepository, pricingInventoryGateway);
        TradeController controller = new TradeController(submitOrderSagaService, tradeQueryService, tradeCartService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TradeHttpExceptionHandler())
                .build();
    }

    @Test
    void getEligibilityReturnsNotFoundWhenProjectionIsMissing() throws Exception {
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("seller-404", "store-404"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/trade/eligibility")
                        .param("sellerId", "seller-404")
                        .param("storefrontId", "store-404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEligibilityReturnsProjectionWhenProjectionExists() throws Exception {
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("seller-1", "store-1"))
                .thenReturn(Optional.of(new EligibilityProjectionEntity(
                        "seller-1",
                        "store-1",
                        "org-1",
                        true,
                        OffsetDateTime.parse("2026-03-31T22:00:00+08:00")
                )));

        mockMvc.perform(get("/api/trade/eligibility")
                        .param("sellerId", "seller-1")
                        .param("storefrontId", "store-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value("seller-1"))
                .andExpect(jsonPath("$.storefrontId").value("store-1"))
                .andExpect(jsonPath("$.organizationId").value("org-1"))
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    void submitReturnsConflictWhenOrderIsRejected() throws Exception {
        when(submitOrderSagaService.submit(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new OrderRejectedException("biz-rejected", "ELIGIBILITY_MISSING",
                        "未找到可用经营资格: seller-404/store-404"));

        mockMvc.perform(post("/api/trade/orders/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessIdempotencyKey": "biz-rejected",
                                  "buyerId": "buyer-1",
                                  "locale": "zh-CN",
                                  "items": [
                                    {
                                      "businessSkuType": "MERCHANT_OFFER_SKU",
                                      "businessSkuId": "sku-1",
                                      "sellerId": "seller-404",
                                      "storefrontId": "store-404",
                                      "quantity": 1,
                                      "unitPrice": 1999
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("未找到可用经营资格: seller-404/store-404"));
    }

    @Test
    void confirmInventoryReturnsReservationReference() throws Exception {
        when(submitOrderSagaService.confirmOrderInventory("order-1"))
                .thenReturn(new PricingInventoryGateway.InventoryReservationResult(
                        "inventory-reservation:biz-1:seller-1",
                        null,
                        0L,
                        "SELLABLE",
                        java.util.List.of()
                ));

        mockMvc.perform(post("/api/trade/orders/order-1/inventory-confirmation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryReservationRef").value("inventory-reservation:biz-1:seller-1"))
                .andExpect(jsonPath("$.sellableState").value("SELLABLE"));
    }

    @Test
    void getOrderPricingSnapshotReturnsSnapshotView() throws Exception {
        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(new com.gmall.trade.infrastructure.persistence.OrderEntity(
                        "order-1",
                        "ORD-biz-1-1",
                        "biz-1",
                        "buyer-1",
                        "seller-1",
                        "MERCHANT",
                        "seller-1",
                        "org-1",
                        "MERCHANT_SELF",
                        "store-1",
                        "CNY",
                        3998L,
                        0L,
                        0L,
                        3998L,
                        "{\"receiver\":\"pending\"}",
                        "pricing-snapshot:biz-1:seller-1",
                        "inventory-reservation:biz-1:seller-1",
                        "SUBMITTED",
                        OffsetDateTime.now()
                )));
        when(pricingInventoryGateway.getSnapshot("biz-1:seller-1"))
                .thenReturn(new PricingInventoryGateway.PricingSnapshotView(
                        "pricing-snapshot:biz-1:seller-1",
                        "biz-1:seller-1",
                        3998L,
                        java.util.List.of(new PricingInventoryGateway.QuotedOrderLine(
                                "MERCHANT_OFFER_SKU",
                                "sku-1",
                                "source-sku-1",
                                1500L,
                                null,
                                1999L,
                                1999L,
                                3998L,
                                "SELLABLE",
                                null
                        ))
                ));

        mockMvc.perform(get("/api/trade/orders/order-1/pricing-snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricingSnapshotRef").value("pricing-snapshot:biz-1:seller-1"))
                .andExpect(jsonPath("$.businessKey").value("biz-1:seller-1"))
                .andExpect(jsonPath("$.items[0].businessSkuType").value("MERCHANT_OFFER_SKU"))
                .andExpect(jsonPath("$.items[0].sourceSkuId").value("source-sku-1"));
    }
}
