package com.gmall.pricinginventory.application;

public interface OrganizationContextGateway {

    OrganizationContext getOrganizationContext(String organizationId);

    record OrganizationContext(String organizationId,
                               String organizationType,
                               String status,
                               String defaultLocale,
                               String supportedLocales,
                               boolean directSupplierQualified,
                               String directSupplierQualificationStatus) {
    }
}
