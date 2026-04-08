package com.gmall.pricinginventory.interfaces.http;

import com.gmall.pricinginventory.application.PricingInventoryModels;
import com.gmall.pricinginventory.application.PricingInventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing-inventory")
public class PricingInventoryController {

    private final PricingInventoryService pricingInventoryService;

    public PricingInventoryController(PricingInventoryService pricingInventoryService) {
        this.pricingInventoryService = pricingInventoryService;
    }

    @PostMapping("/admin/source-skus/{sourceSkuId}/supply-prices")
    public PricingInventoryModels.PriceView maintainSupplyPrice(@PathVariable String sourceSkuId,
                                                                @RequestBody PricingInventoryModels.PriceCommand command) {
        return pricingInventoryService.maintainSupplyPrice(sourceSkuId, command);
    }

    @PostMapping("/admin/source-skus/{sourceSkuId}/direct-retail-prices")
    public PricingInventoryModels.PriceView maintainDirectRetailPrice(@PathVariable String sourceSkuId,
                                                                      @RequestBody PricingInventoryModels.PriceCommand command) {
        return pricingInventoryService.maintainDirectRetailPrice(sourceSkuId, command);
    }

    @PostMapping("/merchant/offer-skus/{merchantOfferSkuId}/retail-prices")
    public PricingInventoryModels.PriceView maintainMerchantOfferPrice(@PathVariable String merchantOfferSkuId,
                                                                       @RequestBody PricingInventoryModels.PriceCommand command) {
        return pricingInventoryService.maintainMerchantOfferPrice(merchantOfferSkuId, command);
    }

    @PostMapping("/admin/source-skus/{sourceSkuId}/adjustments")
    public void adjustInventory(@PathVariable String sourceSkuId,
                                @RequestBody PricingInventoryModels.InventoryAdjustmentCommand command) {
        pricingInventoryService.adjustInventory(sourceSkuId, command);
    }

    @PostMapping("/internal/quotes")
    public PricingInventoryModels.QuoteResult quote(@RequestBody PricingInventoryModels.QuoteCommand command) {
        return pricingInventoryService.quote(command);
    }

    @PostMapping("/internal/reservations")
    public PricingInventoryModels.ReservationResult reserve(@RequestBody PricingInventoryModels.ReservationCommand command) {
        return pricingInventoryService.reserve(command);
    }

    @PostMapping("/internal/reservations/{reservationId}/release")
    public PricingInventoryModels.InventoryReservationResult releaseInventory(@PathVariable String reservationId,
                                                                             @RequestBody PricingInventoryModels.InventoryReservationActionCommand command) {
        return pricingInventoryService.releaseInventory(reservationId, command);
    }

    @PostMapping("/internal/reservations/{reservationId}/confirm")
    public PricingInventoryModels.InventoryReservationResult confirmInventory(@PathVariable String reservationId,
                                                                             @RequestBody PricingInventoryModels.InventoryReservationActionCommand command) {
        return pricingInventoryService.confirmInventory(reservationId, command);
    }

    @GetMapping("/internal/snapshots/{businessKey}")
    public PricingInventoryModels.PricingSnapshotView getSnapshot(@PathVariable String businessKey) {
        return pricingInventoryService.getSnapshot(businessKey);
    }

    @GetMapping("/internal/sellable/{businessSkuType}/{businessSkuId}")
    public PricingInventoryModels.SellableView getSellable(@PathVariable String businessSkuType,
                                                           @PathVariable String businessSkuId) {
        return pricingInventoryService.getSellable(businessSkuType, businessSkuId);
    }
}
