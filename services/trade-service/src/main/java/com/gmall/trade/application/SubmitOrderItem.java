package com.gmall.trade.application;

public record SubmitOrderItem(String businessSkuType,
                              String businessSkuId,
                              String sellerId,
                              String storefrontId,
                              int quantity,
                              long unitPrice) {

    public long subtotal() {
        return quantity * unitPrice;
    }
}
