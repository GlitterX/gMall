package com.gmall.foundation.application;

import java.time.OffsetDateTime;

public final class FoundationQueryModels {

    private FoundationQueryModels() {
    }

    public record OrganizationView(String organizationId,
                                   String organizationType,
                                   String organizationName,
                                   String status,
                                   String defaultLocale,
                                   String supportedLocales,
                                   String sourceApplicationId,
                                   long aggregateVersion) {
    }

    public record OrganizationEligibilityView(String organizationId,
                                              String organizationType,
                                              String status,
                                              boolean sellerActivationAllowed,
                                              boolean storefrontProvisionAllowed,
                                              long aggregateVersion) {
    }

    public record OrganizationContextView(String organizationId,
                                          String organizationType,
                                          String status,
                                          String defaultLocale,
                                          String supportedLocales,
                                          boolean directSupplierQualified,
                                          String directSupplierQualificationStatus,
                                          long aggregateVersion) {
    }

    public record SellerView(String sellerId,
                             String organizationId,
                             String sellerType,
                             String status,
                             long aggregateVersion) {
    }

    public record StorefrontView(String storefrontId,
                                 String organizationId,
                                 String sellerId,
                                 String storefrontType,
                                 String status,
                                 String defaultLocale,
                                 String supportedLocales,
                                 long aggregateVersion) {
    }

    public record DirectQualificationView(String qualificationId,
                                          String organizationId,
                                          String status,
                                          long aggregateVersion) {
    }

    public record SupplyRelationView(String relationId,
                                     String supplierOrganizationId,
                                     String merchantOrganizationId,
                                     String status,
                                     long aggregateVersion) {
    }

    public record SupplyRelationResolutionView(String relationId,
                                               String supplierOrganizationId,
                                               String merchantOrganizationId,
                                               String authorizedCatalogScope,
                                               String normalizedAuthorizedCatalogScope,
                                               String supplyPriceRule,
                                               String settlementRule,
                                               String status,
                                               boolean active,
                                               Boolean catalogAuthorized,
                                               String matchedScopeType,
                                               String matchedScopeValue,
                                               String authorizationReason,
                                               long aggregateVersion) {
    }

    public record StorefrontTerminalView(String storefrontId,
                                         String terminalType,
                                         boolean enabled,
                                         String homePageId,
                                         long aggregateVersion) {
    }

    public record StorefrontOperabilityView(String storefrontId,
                                            String terminalType,
                                            String storefrontStatus,
                                            boolean storefrontActive,
                                            boolean permissionBindingReady,
                                            boolean terminalEnabled,
                                            boolean homePageBound,
                                            boolean operable,
                                            String homePageValidationStatus,
                                            boolean homePageValidationPassed,
                                            long storefrontVersion,
                                            long terminalVersion,
                                            long bindingVersion,
                                            boolean allowed) {
    }

    public record AdmissionReviewTaskView(String taskId,
                                          String applicationId,
                                          long taskSequence,
                                          String taskStatus,
                                          String reviewerId,
                                          OffsetDateTime startedAt,
                                          OffsetDateTime deadlineAt,
                                          OffsetDateTime closedAt,
                                          String lastOperatedBy,
                                          String lastOperationReason,
                                          OffsetDateTime lastOperatedAt,
                                          long aggregateVersion) {
    }

    public record AdmissionReviewTaskQueueItemView(String taskId,
                                                   String applicationId,
                                                   long taskSequence,
                                                   String taskStatus,
                                                   String reviewerId,
                                                   OffsetDateTime deadlineAt,
                                                   OffsetDateTime lastOperatedAt,
                                                   String organizationType,
                                                   String applicantName,
                                                   String applicationStatus,
                                                   String slaStatus) {
    }

    public record AdmissionPendingApplicationView(String applicationId,
                                                  String organizationType,
                                                  String applicantName,
                                                  String contactName,
                                                  String contactMobile,
                                                  OffsetDateTime submittedAt,
                                                  String applicationStatus) {
    }

    public record AdmissionWorkbenchOverviewView(long pendingApplicationCount,
                                                 long activeTaskCount,
                                                 long nearDueTaskCount,
                                                 long overdueTaskCount) {
    }
}
