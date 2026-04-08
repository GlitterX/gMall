package com.gmall.trade.interfaces.http;

import com.gmall.trade.application.SubmitOrderCommand;
import com.gmall.trade.application.SubmitOrderResult;
import com.gmall.trade.application.PricingInventoryGateway.InventoryReservationResult;
import com.gmall.trade.application.PricingInventoryGateway.PricingSnapshotView;
import com.gmall.trade.application.SubmitOrderSagaService;
import com.gmall.trade.application.TradeCartModels.AddCartItemCommand;
import com.gmall.trade.application.TradeCartModels.CartItemView;
import com.gmall.trade.application.TradeCartModels.CartView;
import com.gmall.trade.application.TradeCartModels.CheckoutPreviewCommand;
import com.gmall.trade.application.TradeCartModels.CheckoutPreviewView;
import com.gmall.trade.application.TradeCartModels.UpdateCartItemCommand;
import com.gmall.trade.application.TradeCartService;
import com.gmall.trade.application.TradeQueryModels.EligibilityView;
import com.gmall.trade.application.TradeQueryModels.OrderView;
import com.gmall.trade.application.TradeQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private final SubmitOrderSagaService submitOrderSagaService;
    private final TradeQueryService tradeQueryService;
    private final TradeCartService tradeCartService;

    public TradeController(SubmitOrderSagaService submitOrderSagaService,
                           TradeQueryService tradeQueryService,
                           TradeCartService tradeCartService) {
        this.submitOrderSagaService = submitOrderSagaService;
        this.tradeQueryService = tradeQueryService;
        this.tradeCartService = tradeCartService;
    }

    @PostMapping("/orders/submit")
    public SubmitOrderResult submit(@Valid @RequestBody SubmitOrderCommand command) {
        return submitOrderSagaService.submit(command);
    }

    @GetMapping("/orders/{orderId}")
    public OrderView getOrder(@PathVariable String orderId) {
        return tradeQueryService.getOrder(orderId);
    }

    @GetMapping("/orders/{orderId}/pricing-snapshot")
    public PricingSnapshotView getOrderPricingSnapshot(@PathVariable String orderId) {
        return tradeQueryService.getOrderPricingSnapshot(orderId);
    }

    @PostMapping("/orders/{orderId}/inventory-confirmation")
    public InventoryReservationResult confirmInventory(@PathVariable String orderId) {
        return submitOrderSagaService.confirmOrderInventory(orderId);
    }

    @GetMapping("/cart")
    public CartView getCart(@RequestParam String buyerId) {
        return tradeCartService.getCart(buyerId);
    }

    @PostMapping("/cart/items")
    public CartItemView addCartItem(@RequestBody AddCartItemCommand command) {
        return tradeCartService.addCartItem(command);
    }

    @PatchMapping("/cart/items/{cartItemId}")
    public CartItemView updateCartItem(@PathVariable String cartItemId,
                                       @RequestBody UpdateCartItemCommand command) {
        return tradeCartService.updateCartItem(cartItemId, command);
    }

    @DeleteMapping("/cart/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCartItem(@PathVariable String cartItemId) {
        tradeCartService.deleteCartItem(cartItemId);
    }

    @PostMapping("/checkout/preview")
    public CheckoutPreviewView previewCheckout(@RequestBody CheckoutPreviewCommand command) {
        return tradeCartService.previewCheckout(command.buyerId());
    }

    @GetMapping("/eligibility")
    public EligibilityView getEligibility(@RequestParam String sellerId, @RequestParam String storefrontId) {
        return tradeQueryService.getEligibility(sellerId, storefrontId);
    }
}
