package com.gmall.trade.application;

public final class TradeQueryModels {

    private TradeQueryModels() {
    }

    public record OrderView(String orderId,
                            String orderNo,
                            String buyerId,
                            String sellerId,
                            String sellerOfRecord,
                            String settlementBeneficiary,
                            String orderScene,
                            String storefrontId,
                            long payableAmount,
                            String status) {
    }

    public record EligibilityView(String sellerId,
                                  String storefrontId,
                                  String organizationId,
                                  boolean eligible) {
    }
}
