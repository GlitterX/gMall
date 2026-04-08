package com.gmall.pricinginventory.application;

import java.util.List;
import java.time.OffsetDateTime;

public final class PricingInventoryModels {

    private PricingInventoryModels() {
    }

    public record BusinessOrderLine(String businessSkuType, String businessSkuId, int quantity) {
    }

    public record PriceCommand(long amount,
                               String currencyCode,
                               String idempotencyKey,
                               OffsetDateTime effectiveAt) {
    }

    public record InventoryAdjustmentCommand(long deltaQuantity,
                                             String businessKey,
                                             String reasonCode) {
    }

    public record PriceView(String priceType,
                            String businessSkuType,
                            String businessSkuId,
                            long amount,
                            String currencyCode,
                            String status,
                            OffsetDateTime effectiveAt) {
    }

    public record QuoteCommand(String businessKey, List<BusinessOrderLine> items) {
    }

    public record QuotedOrderLine(String businessSkuType,
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

    public record QuoteResult(long totalAmount,
                              List<QuotedOrderLine> items) {
    }

    public record ReservationCommand(String businessKey, List<BusinessOrderLine> items) {
    }

    public record ReservationResult(String inventoryReservationRef,
                                    String pricingSnapshotRef,
                                    long totalAmount,
                                    String sellableState,
                                    List<QuotedOrderLine> items) {
    }

    public record InventoryReservationActionCommand(String businessKey) {
    }

    public record InventoryReservationResult(String inventoryReservationRef,
                                             String sellableState) {
    }

    public record SellableView(String businessSkuType,
                               String businessSkuId,
                               String sourceSkuId,
                               String sellableState,
                               String reasonCode) {
    }

    public record PricingSnapshotView(String pricingSnapshotRef,
                                      String businessKey,
                                      long totalAmount,
                                      List<QuotedOrderLine> items) {
    }
}
