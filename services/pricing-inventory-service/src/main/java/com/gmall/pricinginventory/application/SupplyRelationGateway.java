package com.gmall.pricinginventory.application;

public interface SupplyRelationGateway {

    SupplyRelationContext resolveSupplyRelation(String supplierOrganizationId,
                                                String merchantOrganizationId,
                                                String sourceProductId);

    record SupplyRelationContext(String relationId,
                                 String status,
                                 boolean active,
                                 boolean catalogAuthorized,
                                 String authorizationReason) {
    }
}
