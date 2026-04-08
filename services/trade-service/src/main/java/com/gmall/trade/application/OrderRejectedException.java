package com.gmall.trade.application;

public class OrderRejectedException extends RuntimeException {

    private final String businessIdempotencyKey;
    private final String reasonCode;

    public OrderRejectedException(String businessIdempotencyKey, String reasonCode, String message) {
        super(message);
        this.businessIdempotencyKey = businessIdempotencyKey;
        this.reasonCode = reasonCode;
    }

    public String businessIdempotencyKey() {
        return businessIdempotencyKey;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
