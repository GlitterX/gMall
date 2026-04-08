package com.gmall.trade.application;

public class EligibilityNotFoundException extends RuntimeException {

    public EligibilityNotFoundException(String sellerId, String storefrontId) {
        super("经营资格不存在: " + sellerId + "/" + storefrontId);
    }
}
