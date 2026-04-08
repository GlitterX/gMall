package com.gmall.trade.interfaces.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmall.trade.application.PricingInventoryGateway;
import com.gmall.trade.application.SubmitOrderSagaService;
import com.gmall.trade.application.TradeCartModels;
import com.gmall.trade.application.TradeCartService;
import com.gmall.trade.application.TradeQueryService;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionRepository;
import com.gmall.trade.infrastructure.persistence.OrderRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TradeCartControllerTest {

    private final SubmitOrderSagaService submitOrderSagaService = mock(SubmitOrderSagaService.class);
    private final TradeCartService tradeCartService = mock(TradeCartService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final EligibilityProjectionRepository eligibilityProjectionRepository = mock(EligibilityProjectionRepository.class);
    private final PricingInventoryGateway pricingInventoryGateway = mock(PricingInventoryGateway.class);

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
    void getCartReturnsBuyerItems() throws Exception {
        when(tradeCartService.getCart("buyer-1"))
                .thenReturn(new TradeCartModels.CartView(
                        "buyer-1",
                        List.of(new TradeCartModels.CartItemView("cart-1", "buyer-1", "MERCHANT_OFFER_SKU", "sku-1", "seller-1", "store-1", 2, true))
                ));

        mockMvc.perform(get("/api/trade/cart").param("buyerId", "buyer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyerId").value("buyer-1"))
                .andExpect(jsonPath("$.items[0].cartItemId").value("cart-1"));
    }

    @Test
    void addCartItemReturnsCartItemView() throws Exception {
        when(tradeCartService.addCartItem(any()))
                .thenReturn(new TradeCartModels.CartItemView("cart-2", "buyer-1", "MERCHANT_OFFER_SKU", "sku-2", "seller-1", "store-1", 3, true));

        mockMvc.perform(post("/api/trade/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buyerId": "buyer-1",
                                  "businessSkuType": "MERCHANT_OFFER_SKU",
                                  "businessSkuId": "sku-2",
                                  "sellerId": "seller-1",
                                  "storefrontId": "store-1",
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItemId").value("cart-2"))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void updateCartItemReturnsUpdatedView() throws Exception {
        when(tradeCartService.updateCartItem(any(), any()))
                .thenReturn(new TradeCartModels.CartItemView("cart-3", "buyer-1", "SOURCE_SKU", "sku-3", "seller-1", "store-1", 5, false));

        mockMvc.perform(patch("/api/trade/cart/items/cart-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5,
                                  "selected": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItemId").value("cart-3"))
                .andExpect(jsonPath("$.selected").value(false));
    }

    @Test
    void deleteCartItemReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/trade/cart/items/cart-4"))
                .andExpect(status().isNoContent());
    }

    @Test
    void previewCheckoutReturnsGroupedSummary() throws Exception {
        when(tradeCartService.previewCheckout("buyer-1"))
                .thenReturn(new TradeCartModels.CheckoutPreviewView(
                        "buyer-1",
                        5500L,
                        List.of(new TradeCartModels.SellerCheckoutGroupView(
                                "seller-1",
                                "store-1",
                                4000L,
                                List.of(new TradeCartModels.CartItemPreviewView("cart-1", "MERCHANT_OFFER_SKU", "sku-1", 2, 2000L, 4000L))
                        ))
                ));

        mockMvc.perform(post("/api/trade/checkout/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buyerId": "buyer-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyerId").value("buyer-1"))
                .andExpect(jsonPath("$.totalAmount").value(5500))
                .andExpect(jsonPath("$.groups[0].sellerId").value("seller-1"));
    }
}
