package com.gmall.trade.application;

import java.util.List;

public interface PricingInventoryGateway {

    QuoteResult quoteForOrder(String businessKey, List<OrderLine> items);

    InventoryReservationResult reserveInventory(String businessKey, List<OrderLine> items);

    InventoryReservationResult releaseInventory(String inventoryReservationRef, String businessKey);

    InventoryReservationResult confirmInventory(String inventoryReservationRef, String businessKey);

    PricingSnapshotView getSnapshot(String businessKey);

    record OrderLine(String businessSkuType, String businessSkuId, int quantity) {
    }

    record QuotedOrderLine(String businessSkuType,
                           String businessSkuId,
                           String sourceSkuId,
                           Long supplyPrice,
                           Long directRetailPrice,
                           Long merchantRetailPrice,
                           long unitPrice,
                           long subtotal,
                           String sellableState,
                           String reasonCode) {
    }

    record QuoteResult(long totalAmount,
                       List<QuotedOrderLine> items) {
    }

    record InventoryReservationResult(String inventoryReservationRef,
                                      String pricingSnapshotRef,
                                      long totalAmount,
                                      String sellableState,
                                      List<QuotedOrderLine> items) {
    }

    record PricingSnapshotView(String pricingSnapshotRef,
                               String businessKey,
                               long totalAmount,
                               List<QuotedOrderLine> items) {
    }
}
