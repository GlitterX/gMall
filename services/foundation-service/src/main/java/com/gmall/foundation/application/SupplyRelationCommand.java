package com.gmall.foundation.application;

public record SupplyRelationCommand(
        String relationId,
        String supplierOrganizationId,
        String merchantOrganizationId,
        String authorizedCatalogScope,
        String supplyPriceRule,
        String settlementRule,
        String operatorId,
        String operationReason
) {
}
