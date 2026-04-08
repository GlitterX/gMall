package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.OrganizationStatus;
import com.gmall.foundation.domain.model.SellerStatus;
import com.gmall.foundation.domain.model.StorefrontStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationRepository;
import com.gmall.foundation.infrastructure.messaging.FoundationEventPayload;
import com.gmall.foundation.infrastructure.persistence.OrganizationEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationRepository;
import com.gmall.foundation.infrastructure.persistence.SellerEntity;
import com.gmall.foundation.infrastructure.persistence.SellerRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoundationGovernanceService {

    private final OrganizationRepository organizationRepository;
    private final DirectSupplierQualificationRepository directSupplierQualificationRepository;
    private final SellerRepository sellerRepository;
    private final StorefrontRepository storefrontRepository;
    private final FoundationEventAppender foundationEventAppender;

    public FoundationGovernanceService(OrganizationRepository organizationRepository,
                                       DirectSupplierQualificationRepository directSupplierQualificationRepository,
                                       SellerRepository sellerRepository,
                                       StorefrontRepository storefrontRepository,
                                       FoundationEventAppender foundationEventAppender) {
        this.organizationRepository = organizationRepository;
        this.directSupplierQualificationRepository = directSupplierQualificationRepository;
        this.sellerRepository = sellerRepository;
        this.storefrontRepository = storefrontRepository;
        this.foundationEventAppender = foundationEventAppender;
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.OrganizationView getOrganization(String organizationId) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + organizationId));
        return new FoundationQueryModels.OrganizationView(
                organization.getOrganizationId(),
                organization.getOrganizationType(),
                organization.getOrganizationName(),
                organization.getStatus().name(),
                organization.getDefaultLocale(),
                organization.getSupportedLocales(),
                organization.getSourceApplicationId(),
                organization.getAggregateVersion()
        );
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.OrganizationEligibilityView getOrganizationEligibility(String organizationId) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + organizationId));
        boolean organizationActive = organization.getStatus() == OrganizationStatus.ACTIVE;
        boolean sellerActivationAllowed = organizationActive && !"PLATFORM".equals(organization.getOrganizationType());
        boolean storefrontProvisionAllowed = organizationActive;
        return new FoundationQueryModels.OrganizationEligibilityView(
                organization.getOrganizationId(),
                organization.getOrganizationType(),
                organization.getStatus().name(),
                sellerActivationAllowed,
                storefrontProvisionAllowed,
                organization.getAggregateVersion()
        );
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.OrganizationContextView getOrganizationContext(String organizationId) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + organizationId));
        return directSupplierQualificationRepository.findByOrganizationId(organizationId)
                .map(qualification -> new FoundationQueryModels.OrganizationContextView(
                        organization.getOrganizationId(),
                        organization.getOrganizationType(),
                        organization.getStatus().name(),
                        organization.getDefaultLocale(),
                        organization.getSupportedLocales(),
                        "ACTIVE".equals(qualification.getStatus().name()),
                        qualification.getStatus().name(),
                        organization.getAggregateVersion()
                ))
                .orElseGet(() -> new FoundationQueryModels.OrganizationContextView(
                        organization.getOrganizationId(),
                        organization.getOrganizationType(),
                        organization.getStatus().name(),
                        organization.getDefaultLocale(),
                        organization.getSupportedLocales(),
                        false,
                        "NOT_FOUND",
                        organization.getAggregateVersion()
                ));
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.SellerView getSeller(String sellerId) {
        SellerEntity seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("卖家不存在: " + sellerId));
        return new FoundationQueryModels.SellerView(
                seller.getSellerId(),
                seller.getOrganizationId(),
                seller.getSellerType(),
                seller.getStatus().name(),
                seller.getAggregateVersion()
        );
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.StorefrontView getStorefront(String storefrontId) {
        StorefrontEntity storefront = storefrontRepository.findById(storefrontId)
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在: " + storefrontId));
        return new FoundationQueryModels.StorefrontView(
                storefront.getStorefrontId(),
                storefront.getOrganizationId(),
                storefront.getSellerId(),
                storefront.getStorefrontType(),
                storefront.getStatus().name(),
                storefront.getDefaultLocale(),
                storefront.getSupportedLocales(),
                storefront.getAggregateVersion()
        );
    }

    @Transactional
    public FoundationQueryModels.OrganizationView freezeOrganization(String organizationId,
                                                                     FoundationOperationCommand command) {
        return changeOrganizationStatus(organizationId, OrganizationStatus.FROZEN, "OrganizationFrozen", command);
    }

    @Transactional
    public FoundationQueryModels.OrganizationView restoreOrganization(String organizationId,
                                                                      FoundationOperationCommand command) {
        return changeOrganizationStatus(organizationId, OrganizationStatus.ACTIVE, "OrganizationActivated", command);
    }

    @Transactional
    public FoundationQueryModels.OrganizationView closeOrganization(String organizationId,
                                                                    FoundationOperationCommand command) {
        return changeOrganizationStatus(organizationId, OrganizationStatus.CLOSED, "OrganizationClosed", command);
    }

    @Transactional
    public FoundationQueryModels.SellerView suspendSeller(String sellerId, FoundationOperationCommand command) {
        return changeSellerStatus(sellerId, SellerStatus.SUSPENDED, "SellerSuspended", command);
    }

    @Transactional
    public FoundationQueryModels.SellerView restoreSeller(String sellerId, FoundationOperationCommand command) {
        return changeSellerStatus(sellerId, SellerStatus.ACTIVE, "SellerActivated", command);
    }

    private FoundationQueryModels.SellerView changeSellerStatus(String sellerId,
                                                                SellerStatus status,
                                                                String eventType,
                                                                FoundationOperationCommand command) {
        SellerEntity seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("卖家不存在: " + sellerId));
        seller.changeStatus(
                status,
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        foundationEventAppender.append(
                "Seller",
                seller.getSellerId(),
                eventType,
                seller.getAggregateVersion(),
                new FoundationEventPayload(
                        seller.getOrganizationId(),
                        seller.getSellerId(),
                        null,
                        seller.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                )
        );
        return getSeller(sellerId);
    }

    @Transactional
    public FoundationQueryModels.StorefrontView freezeStorefront(String storefrontId, FoundationOperationCommand command) {
        return changeStorefrontStatus(storefrontId, StorefrontStatus.FROZEN, "StorefrontFrozen", command);
    }

    @Transactional
    public FoundationQueryModels.StorefrontView restoreStorefront(String storefrontId, FoundationOperationCommand command) {
        return changeStorefrontStatus(storefrontId, StorefrontStatus.ACTIVE, "StorefrontActivated", command);
    }

    @Transactional
    public FoundationQueryModels.StorefrontView closeStorefront(String storefrontId, FoundationOperationCommand command) {
        return changeStorefrontStatus(storefrontId, StorefrontStatus.CLOSED, "StorefrontClosed", command);
    }

    private FoundationQueryModels.OrganizationView changeOrganizationStatus(String organizationId,
                                                                            OrganizationStatus status,
                                                                            String eventType,
                                                                            FoundationOperationCommand command) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + organizationId));
        organization.changeStatus(status, command.operatorId(), command.operationReason(), OffsetDateTime.now());
        foundationEventAppender.append(
                "Organization",
                organization.getOrganizationId(),
                eventType,
                organization.getAggregateVersion(),
                new FoundationEventPayload(
                        organization.getOrganizationId(),
                        null,
                        null,
                        organization.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                )
        );
        return getOrganization(organizationId);
    }

    private FoundationQueryModels.StorefrontView changeStorefrontStatus(String storefrontId,
                                                                        StorefrontStatus status,
                                                                        String eventType,
                                                                        FoundationOperationCommand command) {
        StorefrontEntity storefront = storefrontRepository.findById(storefrontId)
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在: " + storefrontId));
        storefront.changeStatus(
                status,
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        foundationEventAppender.append(
                "Storefront",
                storefront.getStorefrontId(),
                eventType,
                storefront.getAggregateVersion(),
                new FoundationEventPayload(
                        storefront.getOrganizationId(),
                        storefront.getSellerId(),
                        storefront.getStorefrontId(),
                        storefront.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                )
        );
        return getStorefront(storefrontId);
    }
}
