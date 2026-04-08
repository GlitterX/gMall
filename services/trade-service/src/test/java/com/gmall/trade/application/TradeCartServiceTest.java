package com.gmall.trade.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmall.trade.infrastructure.persistence.CartItemEntity;
import com.gmall.trade.infrastructure.persistence.CartItemRepository;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionEntity;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradeCartServiceTest {

    private final CartItemRepository cartItemRepository = mock(CartItemRepository.class);
    private final EligibilityProjectionRepository eligibilityProjectionRepository = mock(EligibilityProjectionRepository.class);
    private final PricingInventoryGateway pricingInventoryGateway = mock(PricingInventoryGateway.class);

    private final List<CartItemEntity> savedCartItems = new ArrayList<>();

    private TradeCartService tradeCartService;

    @BeforeEach
    void setUp() {
        tradeCartService = new TradeCartService(cartItemRepository, eligibilityProjectionRepository, pricingInventoryGateway);
        when(cartItemRepository.save(any(CartItemEntity.class))).thenAnswer(invocation -> {
            CartItemEntity entity = invocation.getArgument(0);
            savedCartItems.removeIf(item -> item.getCartItemId().equals(entity.getCartItemId()));
            savedCartItems.add(entity);
            return entity;
        });
        doAnswer(invocation -> {
            CartItemEntity entity = invocation.getArgument(0);
            savedCartItems.removeIf(item -> item.getCartItemId().equals(entity.getCartItemId()));
            return null;
        }).when(cartItemRepository).delete(any(CartItemEntity.class));
        when(cartItemRepository.findByBuyerIdOrderByUpdatedAtDesc("buyer-1"))
                .thenAnswer(invocation -> savedCartItems.stream()
                        .filter(item -> "buyer-1".equals(item.getBuyerId()))
                        .sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt()))
                        .toList());
    }

    @Test
    void addCartItemCreatesNewCartRow() {
        when(cartItemRepository.findByBuyerIdAndBusinessSkuTypeAndBusinessSkuIdAndSellerIdAndStorefrontId(
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-1",
                "seller-1",
                "store-1"
        ))
                .thenReturn(Optional.empty());

        TradeCartModels.CartItemView item = tradeCartService.addCartItem(new TradeCartModels.AddCartItemCommand(
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-1",
                "seller-1",
                "store-1",
                2
        ));

        assertThat(item.buyerId()).isEqualTo("buyer-1");
        assertThat(item.businessSkuType()).isEqualTo("MERCHANT_OFFER_SKU");
        assertThat(item.businessSkuId()).isEqualTo("sku-1");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.selected()).isTrue();
    }

    @Test
    void addCartItemMergesQuantityWhenSameSkuAlreadyExists() {
        CartItemEntity existing = new CartItemEntity(
                "cart-1",
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-1",
                "seller-1",
                "store-1",
                1,
                true,
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00")
        );
        when(cartItemRepository.findByBuyerIdAndBusinessSkuTypeAndBusinessSkuIdAndSellerIdAndStorefrontId(
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-1",
                "seller-1",
                "store-1"
        ))
                .thenReturn(Optional.of(existing));

        TradeCartModels.CartItemView item = tradeCartService.addCartItem(new TradeCartModels.AddCartItemCommand(
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-1",
                "seller-1",
                "store-1",
                3
        ));

        assertThat(item.cartItemId()).isEqualTo("cart-1");
        assertThat(item.quantity()).isEqualTo(4);
    }

    @Test
    void updateCartItemChangesQuantityAndSelection() {
        CartItemEntity existing = new CartItemEntity(
                "cart-2",
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-2",
                "seller-1",
                "store-1",
                2,
                true,
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00")
        );
        when(cartItemRepository.findById("cart-2")).thenReturn(Optional.of(existing));

        TradeCartModels.CartItemView item = tradeCartService.updateCartItem("cart-2", new TradeCartModels.UpdateCartItemCommand(5, false));

        assertThat(item.cartItemId()).isEqualTo("cart-2");
        assertThat(item.quantity()).isEqualTo(5);
        assertThat(item.selected()).isFalse();
    }

    @Test
    void deleteCartItemRemovesRow() {
        CartItemEntity existing = new CartItemEntity(
                "cart-3",
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-3",
                "seller-1",
                "store-1",
                1,
                true,
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00")
        );
        savedCartItems.add(existing);
        when(cartItemRepository.findById("cart-3")).thenReturn(Optional.of(existing));

        tradeCartService.deleteCartItem("cart-3");

        assertThat(savedCartItems).isEmpty();
    }

    @Test
    void getCartReturnsBuyerItems() {
        savedCartItems.add(new CartItemEntity(
                "cart-4",
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-4",
                "seller-1",
                "store-1",
                1,
                true,
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T11:05:00+08:00")
        ));

        TradeCartModels.CartView cart = tradeCartService.getCart("buyer-1");

        assertThat(cart.buyerId()).isEqualTo("buyer-1");
        assertThat(cart.items()).singleElement().satisfies(item -> {
            assertThat(item.cartItemId()).isEqualTo("cart-4");
            assertThat(item.businessSkuType()).isEqualTo("MERCHANT_OFFER_SKU");
            assertThat(item.businessSkuId()).isEqualTo("sku-4");
        });
    }

    @Test
    void checkoutPreviewGroupsSelectedItemsBySeller() {
        savedCartItems.add(new CartItemEntity(
                "cart-5",
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-5",
                "seller-1",
                "store-1",
                2,
                true,
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T11:05:00+08:00")
        ));
        savedCartItems.add(new CartItemEntity(
                "cart-6",
                "buyer-1",
                "SOURCE_SKU",
                "sku-6",
                "seller-2",
                "store-2",
                1,
                true,
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T11:06:00+08:00")
        ));
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("seller-1", "store-1"))
                .thenReturn(Optional.of(new EligibilityProjectionEntity("seller-1", "store-1", "org-1", true, OffsetDateTime.now())));
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("seller-2", "store-2"))
                .thenReturn(Optional.of(new EligibilityProjectionEntity("seller-2", "store-2", "org-2", true, OffsetDateTime.now())));
        when(pricingInventoryGateway.quoteForOrder(
                "cart-preview:buyer-1:seller-1",
                List.of(new PricingInventoryGateway.OrderLine("MERCHANT_OFFER_SKU", "sku-5", 2))
        ))
                .thenReturn(new PricingInventoryGateway.QuoteResult(
                        4000L,
                        List.of(new PricingInventoryGateway.QuotedOrderLine(
                                "MERCHANT_OFFER_SKU",
                                "sku-5",
                                "source-sku-5",
                                1500L,
                                null,
                                2000L,
                                2000L,
                                4000L,
                                "SELLABLE",
                                null
                        ))
                ));
        when(pricingInventoryGateway.quoteForOrder(
                "cart-preview:buyer-1:seller-2",
                List.of(new PricingInventoryGateway.OrderLine("SOURCE_SKU", "sku-6", 1))
        ))
                .thenReturn(new PricingInventoryGateway.QuoteResult(
                        1500L,
                        List.of(new PricingInventoryGateway.QuotedOrderLine(
                                "SOURCE_SKU",
                                "sku-6",
                                "source-sku-6",
                                900L,
                                1500L,
                                null,
                                1500L,
                                1500L,
                                "SELLABLE",
                                null
                        ))
                ));

        TradeCartModels.CheckoutPreviewView preview = tradeCartService.previewCheckout("buyer-1");

        assertThat(preview.buyerId()).isEqualTo("buyer-1");
        assertThat(preview.totalAmount()).isEqualTo(5500L);
        assertThat(preview.groups()).hasSize(2);
        assertThat(preview.groups()).extracting(TradeCartModels.SellerCheckoutGroupView::sellerId)
                .containsExactly("seller-2", "seller-1");
    }

    @Test
    void checkoutPreviewRejectsWhenSellerEligibilityIsMissing() {
        savedCartItems.add(new CartItemEntity(
                "cart-7",
                "buyer-1",
                "MERCHANT_OFFER_SKU",
                "sku-7",
                "seller-missing",
                "store-missing",
                1,
                true,
                OffsetDateTime.parse("2026-04-08T11:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T11:06:00+08:00")
        ));
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("seller-missing", "store-missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeCartService.previewCheckout("buyer-1"))
                .isInstanceOf(OrderRejectedException.class)
                .hasMessageContaining("seller-missing/store-missing");
    }
}
