package com.gmall.marketingcontent.application;

public class MarketingContentRejectedException extends RuntimeException {

    private final String reasonCode;

    public MarketingContentRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
