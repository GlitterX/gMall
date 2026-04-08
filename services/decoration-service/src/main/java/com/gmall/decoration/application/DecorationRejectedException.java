package com.gmall.decoration.application;

public class DecorationRejectedException extends RuntimeException {

    private final String reasonCode;

    public DecorationRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
