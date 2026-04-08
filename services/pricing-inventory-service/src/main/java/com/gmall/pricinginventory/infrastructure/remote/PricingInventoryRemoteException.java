package com.gmall.pricinginventory.infrastructure.remote;

public class PricingInventoryRemoteException extends RuntimeException {

    private final String reasonCode;

    public PricingInventoryRemoteException(String reasonCode, String message, Throwable cause) {
        super(message, cause);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
