package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.OrganizationStatus;
import com.gmall.foundation.domain.model.SellerStatus;
import com.gmall.foundation.domain.model.StorefrontStatus;
import com.gmall.foundation.domain.model.DirectQualificationStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.messaging.FoundationEventPayload;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationRepository;
import com.gmall.foundation.infrastructure.persistence.OrganizationEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationRepository;
import com.gmall.foundation.infrastructure.persistence.SellerEntity;
import com.gmall.foundation.infrastructure.persistence.SellerRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontPermissionBindingEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontPermissionBindingRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontTerminalEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontTerminalRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantProvisionSagaService {

    private final OrganizationRepository organizationRepository;
    private final SellerRepository sellerRepository;
    private final StorefrontRepository storefrontRepository;
    private final DirectSupplierQualificationRepository qualificationRepository;
    private final StorefrontTerminalRepository storefrontTerminalRepository;
    private final StorefrontPermissionBindingRepository permissionBindingRepository;
    private final FoundationEventAppender foundationEventAppender;
    private final MerchantProvisionSagaHook merchantProvisionSagaHook;

    public MerchantProvisionSagaService(OrganizationRepository organizationRepository,
                                        SellerRepository sellerRepository,
                                        StorefrontRepository storefrontRepository,
                                        DirectSupplierQualificationRepository qualificationRepository,
                                        StorefrontTerminalRepository storefrontTerminalRepository,
                                        StorefrontPermissionBindingRepository permissionBindingRepository,
                                        FoundationEventAppender foundationEventAppender,
                                        MerchantProvisionSagaHook merchantProvisionSagaHook) {
        this.organizationRepository = organizationRepository;
        this.sellerRepository = sellerRepository;
        this.storefrontRepository = storefrontRepository;
        this.qualificationRepository = qualificationRepository;
        this.storefrontTerminalRepository = storefrontTerminalRepository;
        this.permissionBindingRepository = permissionBindingRepository;
        this.foundationEventAppender = foundationEventAppender;
        this.merchantProvisionSagaHook = merchantProvisionSagaHook;
    }

    @Transactional
    public MerchantProvisionResult provision(MerchantProvisionCommand command) {
        MerchantProvisionSagaContext context = new MerchantProvisionSagaContext(
                command.organizationId(),
                command.sellerId(),
                command.storefrontId()
        );
        String sellerType = normalizeSellerType(command.sellerType());
        String storefrontType = "DIRECT_SUPPLIER".equals(sellerType) ? "DIRECT_SUPPLIER" : "MERCHANT";
        if ("DIRECT_SUPPLIER".equals(sellerType) && qualificationRepository.findByOrganizationId(command.organizationId())
                .filter(entity -> entity.getStatus() == DirectQualificationStatus.ACTIVE)
                .isEmpty()) {
            throw new IllegalStateException("直营供应商缺少有效资格");
        }

        OrganizationEntity organization = organizationRepository.findById(command.organizationId())
                .map(existing -> activateOrganization(existing, command))
                .orElseGet(() -> organizationRepository.save(new OrganizationEntity(
                        command.organizationId(),
                        command.organizationType(),
                        command.organizationId(),
                        OrganizationStatus.ACTIVE,
                        null,
                        command.defaultLocale(),
                        command.supportedLocales(),
                        command.operatorId(),
                        command.operationReason(),
                        OffsetDateTime.now(),
                        1L
                )));
        foundationEventAppender.append("Organization", command.organizationId(), "OrganizationActivated",
                organization.getAggregateVersion(),
                new FoundationEventPayload(
                        command.organizationId(),
                        null,
                        null,
                        organization.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                ));
        merchantProvisionSagaHook.afterStep(MerchantProvisionStep.ORGANIZATION_ACTIVATED, context);

        SellerEntity seller = sellerRepository.save(
                new SellerEntity(
                        command.sellerId(),
                        command.organizationId(),
                        sellerType,
                        SellerStatus.ACTIVE,
                        command.operatorId(),
                        command.operationReason(),
                        OffsetDateTime.now(),
                        1L
                )
        );
        foundationEventAppender.append("Seller", command.sellerId(), "SellerActivated", 1L,
                new FoundationEventPayload(
                        command.organizationId(),
                        command.sellerId(),
                        null,
                        seller.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                ));
        merchantProvisionSagaHook.afterStep(MerchantProvisionStep.SELLER_ACTIVATED, context);

        storefrontRepository.save(
                new StorefrontEntity(
                        command.storefrontId(),
                        command.organizationId(),
                        command.sellerId(),
                        storefrontType,
                        StorefrontStatus.ACTIVE,
                        command.defaultLocale(),
                        command.supportedLocales(),
                        command.operatorId(),
                        command.operationReason(),
                        OffsetDateTime.now(),
                        1L
                )
        );
        initializeStorefrontGovernance(command);
        foundationEventAppender.append("Storefront", command.storefrontId(), "StorefrontActivated", 1L,
                new FoundationEventPayload(
                        command.organizationId(),
                        command.sellerId(),
                        command.storefrontId(),
                        StorefrontStatus.ACTIVE.name(),
                        command.operatorId(),
                        command.operationReason()
                ));
        merchantProvisionSagaHook.afterStep(MerchantProvisionStep.STOREFRONT_ACTIVATED, context);

        return new MerchantProvisionResult(command.organizationId(), command.sellerId(), command.storefrontId());
    }

    private OrganizationEntity activateOrganization(OrganizationEntity existing, MerchantProvisionCommand command) {
        if (existing.getStatus() != OrganizationStatus.ACTIVE) {
            existing.changeStatus(
                    OrganizationStatus.ACTIVE,
                    command.operatorId(),
                    command.operationReason(),
                    OffsetDateTime.now()
            );
        }
        return existing;
    }

    private void initializeStorefrontGovernance(MerchantProvisionCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        storefrontTerminalRepository.save(new StorefrontTerminalEntity(
                command.storefrontId(),
                "MOBILE",
                false,
                null,
                command.operatorId(),
                "init",
                now,
                1L
        ));
        storefrontTerminalRepository.save(new StorefrontTerminalEntity(
                command.storefrontId(),
                "PC",
                false,
                null,
                command.operatorId(),
                "init",
                now,
                1L
        ));
        permissionBindingRepository.save(new StorefrontPermissionBindingEntity(
                command.storefrontId(),
                "",
                "",
                1L,
                command.operatorId(),
                "init",
                now
        ));
    }

    private String normalizeSellerType(String sellerType) {
        return sellerType == null || sellerType.isBlank() ? "MERCHANT" : sellerType;
    }
}
