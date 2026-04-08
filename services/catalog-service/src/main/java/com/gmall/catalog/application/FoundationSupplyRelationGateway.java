package com.gmall.catalog.application;

public interface FoundationSupplyRelationGateway {

    AuthorizationDecision resolveSourceProductAuthorization(String supplierOrganizationId,
                                                            String merchantOrganizationId,
                                                            String sourceProductId);

    record AuthorizationDecision(String relationId,
                                 boolean active,
                                 boolean catalogAuthorized,
                                 String authorizationReason) {
    }
}
