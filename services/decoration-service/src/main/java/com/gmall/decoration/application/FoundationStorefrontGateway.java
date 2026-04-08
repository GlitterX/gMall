package com.gmall.decoration.application;

public interface FoundationStorefrontGateway {

    StorefrontView getStorefront(String storefrontId);

    StorefrontOperability getOperability(String storefrontId, String terminalType, String operation);

    record StorefrontView(String storefrontId,
                          String storefrontType,
                          String storefrontStatus,
                          String defaultLocale,
                          String supportedLocales) {
    }

    record StorefrontOperability(String storefrontId,
                                 String terminalType,
                                 String storefrontStatus,
                                 boolean storefrontActive,
                                 boolean permissionBindingReady,
                                 boolean terminalEnabled,
                                 boolean homePageBound,
                                 boolean operable,
                                 String homePageValidationStatus,
                                 boolean homePageValidationPassed,
                                 boolean allowed) {
    }
}
