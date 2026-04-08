package com.gmall.pricinginventory.interfaces.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmall.pricinginventory.application.PricingInventoryModels;
import com.gmall.pricinginventory.application.PricingInventoryRejectedException;
import com.gmall.pricinginventory.application.PricingInventoryService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PricingInventoryControllerTest {

    private final PricingInventoryService pricingInventoryService = mock(PricingInventoryService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PricingInventoryController controller = new PricingInventoryController(pricingInventoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PricingInventoryHttpExceptionHandler())
                .build();
    }

    @Test
    void maintainSupplyPriceReturnsPriceView() throws Exception {
        when(pricingInventoryService.maintainSupplyPrice(any(), any()))
                .thenReturn(new PricingInventoryModels.PriceView(
                        "SUPPLY",
                        "SOURCE_SKU",
                        "source-sku-1",
                        1800L,
                        "CNY",
                        "ACTIVE",
                        OffsetDateTime.parse("2026-04-08T18:00:00+08:00")
                ));

        mockMvc.perform(post("/api/pricing-inventory/admin/source-skus/source-sku-1/supply-prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 1800,
                                  "currencyCode": "CNY",
                                  "idempotencyKey": "idem-1",
                                  "effectiveAt": "2026-04-08T18:00:00+08:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceType").value("SUPPLY"))
                .andExpect(jsonPath("$.businessSkuType").value("SOURCE_SKU"))
                .andExpect(jsonPath("$.businessSkuId").value("source-sku-1"));
    }

    @Test
    void quoteReturnsTypedQuotedItems() throws Exception {
        when(pricingInventoryService.quote(any()))
                .thenReturn(new PricingInventoryModels.QuoteResult(
                        4200L,
                        List.of(new PricingInventoryModels.QuotedOrderLine(
                                "MERCHANT_OFFER_SKU",
                                "offer-sku-1",
                                "source-sku-1",
                                1800L,
                                null,
                                2100L,
                                2100L,
                                4200L,
                                "SELLABLE",
                                "OK"
                        ))
                ));

        mockMvc.perform(post("/api/pricing-inventory/internal/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessKey": "quote-1",
                                  "items": [
                                    {
                                      "businessSkuType": "MERCHANT_OFFER_SKU",
                                      "businessSkuId": "offer-sku-1",
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(4200))
                .andExpect(jsonPath("$.items[0].businessSkuType").value("MERCHANT_OFFER_SKU"))
                .andExpect(jsonPath("$.items[0].merchantRetailPrice").value(2100))
                .andExpect(jsonPath("$.items[0].sellableState").value("SELLABLE"));
    }

    @Test
    void reserveReturnsSnapshotAndReservation() throws Exception {
        when(pricingInventoryService.reserve(any()))
                .thenReturn(new PricingInventoryModels.ReservationResult(
                        "inventory-reservation:reserve-1",
                        "pricing-snapshot:reserve-1",
                        6400L,
                        "SELLABLE",
                        List.of(new PricingInventoryModels.QuotedOrderLine(
                                "SOURCE_SKU",
                                "source-sku-1",
                                "source-sku-1",
                                null,
                                3200L,
                                null,
                                3200L,
                                6400L,
                                "SELLABLE",
                                "OK"
                        ))
                ));

        mockMvc.perform(post("/api/pricing-inventory/internal/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessKey": "reserve-1",
                                  "items": [
                                    {
                                      "businessSkuType": "SOURCE_SKU",
                                      "businessSkuId": "source-sku-1",
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryReservationRef").value("inventory-reservation:reserve-1"))
                .andExpect(jsonPath("$.pricingSnapshotRef").value("pricing-snapshot:reserve-1"))
                .andExpect(jsonPath("$.items[0].directRetailPrice").value(3200));
    }

    @Test
    void getSellableReturnsProjectionView() throws Exception {
        when(pricingInventoryService.getSellable("MERCHANT_OFFER_SKU", "offer-sku-1"))
                .thenReturn(new PricingInventoryModels.SellableView(
                        "MERCHANT_OFFER_SKU",
                        "offer-sku-1",
                        "source-sku-1",
                        "SELLABLE",
                        "OK"
                ));

        mockMvc.perform(get("/api/pricing-inventory/internal/sellable/MERCHANT_OFFER_SKU/offer-sku-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessSkuType").value("MERCHANT_OFFER_SKU"))
                .andExpect(jsonPath("$.sellableState").value("SELLABLE"));
    }

    @Test
    void quoteReturnsConflictWhenPricingRejected() throws Exception {
        when(pricingInventoryService.quote(any()))
                .thenThrow(new PricingInventoryRejectedException("PRICE_MISSING", "PRICE_MISSING: offer-sku-404"));

        mockMvc.perform(post("/api/pricing-inventory/internal/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessKey": "quote-404",
                                  "items": [
                                    {
                                      "businessSkuType": "MERCHANT_OFFER_SKU",
                                      "businessSkuId": "offer-sku-404",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reasonCode").value("PRICE_MISSING"));
    }
}
