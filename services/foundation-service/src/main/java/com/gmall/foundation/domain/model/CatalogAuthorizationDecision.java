package com.gmall.foundation.domain.model;

public record CatalogAuthorizationDecision(Boolean catalogAuthorized,
                                           String matchedScopeType,
                                           String matchedScopeValue,
                                           String authorizationReason) {

    public static CatalogAuthorizationDecision authorized(String matchedScopeType,
                                                          String matchedScopeValue,
                                                          String authorizationReason) {
        return new CatalogAuthorizationDecision(true, matchedScopeType, matchedScopeValue, authorizationReason);
    }

    public static CatalogAuthorizationDecision rejected(String authorizationReason) {
        return new CatalogAuthorizationDecision(false, "NONE", null, authorizationReason);
    }

    public static CatalogAuthorizationDecision notEvaluated(String authorizationReason) {
        return new CatalogAuthorizationDecision(null, "NONE", null, authorizationReason);
    }
}
