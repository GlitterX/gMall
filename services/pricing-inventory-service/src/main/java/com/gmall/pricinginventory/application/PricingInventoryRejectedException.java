package com.gmall.pricinginventory.application;

public class PricingInventoryRejectedException extends RuntimeException {

    private final String reasonCode;

    public PricingInventoryRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
