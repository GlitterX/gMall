package com.gmall.trade.application;

import java.util.List;

public final class TradeCartModels {

    private TradeCartModels() {
    }

    public record AddCartItemCommand(String buyerId,
                                     String businessSkuType,
                                     String businessSkuId,
                                     String sellerId,
                                     String storefrontId,
                                     int quantity) {
    }

    public record UpdateCartItemCommand(int quantity, boolean selected) {
    }

    public record CheckoutPreviewCommand(String buyerId) {
    }

    public record CartItemView(String cartItemId,
                               String buyerId,
                               String businessSkuType,
                               String businessSkuId,
                               String sellerId,
                               String storefrontId,
                               int quantity,
                               boolean selected) {
    }

    public record CartView(String buyerId, List<CartItemView> items) {
    }

    public record CartItemPreviewView(String cartItemId,
                                      String businessSkuType,
                                      String businessSkuId,
                                      int quantity,
                                      long unitPrice,
                                      long subtotal) {
    }

    public record SellerCheckoutGroupView(String sellerId,
                                          String storefrontId,
                                          long payableAmount,
                                          List<CartItemPreviewView> items) {
    }

    public record CheckoutPreviewView(String buyerId,
                                      long totalAmount,
                                      List<SellerCheckoutGroupView> groups) {
    }
}
