package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.DirectQualificationStatus;
import com.gmall.foundation.domain.model.AuthorizedCatalogScope;
import com.gmall.foundation.domain.model.CatalogAuthorizationDecision;
import com.gmall.foundation.domain.model.SupplyRelationStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.messaging.FoundationEventPayload;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationEntity;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationRepository;
import com.gmall.foundation.infrastructure.persistence.SupplyRelationEntity;
import com.gmall.foundation.infrastructure.persistence.SupplyRelationRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoundationRelationshipService {

    private final DirectSupplierQualificationRepository qualificationRepository;
    private final SupplyRelationRepository supplyRelationRepository;
    private final FoundationEventAppender foundationEventAppender;

    public FoundationRelationshipService(DirectSupplierQualificationRepository qualificationRepository,
                                         SupplyRelationRepository supplyRelationRepository,
                                         FoundationEventAppender foundationEventAppender) {
        this.qualificationRepository = qualificationRepository;
        this.supplyRelationRepository = supplyRelationRepository;
        this.foundationEventAppender = foundationEventAppender;
    }

    @Transactional
    public FoundationQueryModels.DirectQualificationView createQualification(DirectSupplierQualificationCommand command) {
        DirectSupplierQualificationEntity qualification = qualificationRepository.save(
                new DirectSupplierQualificationEntity(
                        command.qualificationId(),
                        command.organizationId(),
                        DirectQualificationStatus.PENDING,
                        command.operatorId(),
                        command.operationReason(),
                        OffsetDateTime.now(),
                        1L
                )
        );
        return toView(qualification);
    }

    @Transactional
    public FoundationQueryModels.DirectQualificationView approveQualification(String qualificationId,
                                                                              FoundationOperationCommand command) {
        DirectSupplierQualificationEntity qualification = qualificationRepository.findById(qualificationId)
                .orElseThrow(() -> new IllegalArgumentException("直营资格不存在: " + qualificationId));
        qualification.changeStatus(
                DirectQualificationStatus.ACTIVE,
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        qualificationRepository.save(qualification);
        foundationEventAppender.append(
                "DirectSupplierQualification",
                qualificationId,
                "DirectSupplierQualificationActivated",
                qualification.getAggregateVersion(),
                new FoundationEventPayload(
                        qualification.getOrganizationId(),
                        null,
                        null,
                        qualification.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                )
        );
        return toView(qualification);
    }

    @Transactional
    public FoundationQueryModels.DirectQualificationView restoreQualification(String qualificationId,
                                                                              FoundationOperationCommand command) {
        return approveQualification(qualificationId, command);
    }

    @Transactional
    public FoundationQueryModels.DirectQualificationView suspendQualification(String qualificationId,
                                                                              FoundationOperationCommand command) {
        DirectSupplierQualificationEntity qualification = qualificationRepository.findById(qualificationId)
                .orElseThrow(() -> new IllegalArgumentException("直营资格不存在: " + qualificationId));
        qualification.changeStatus(
                DirectQualificationStatus.SUSPENDED,
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        qualificationRepository.save(qualification);
        foundationEventAppender.append(
                "DirectSupplierQualification",
                qualificationId,
                "DirectSupplierQualificationDeactivated",
                qualification.getAggregateVersion(),
                new FoundationEventPayload(
                        qualification.getOrganizationId(),
                        null,
                        null,
                        qualification.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                )
        );
        return toView(qualification);
    }

    @Transactional
    public FoundationQueryModels.SupplyRelationView createSupplyRelation(SupplyRelationCommand command) {
        String normalizedAuthorizedCatalogScope = AuthorizedCatalogScope.parse(command.authorizedCatalogScope())
                .normalizedValue();
        SupplyRelationEntity relation = supplyRelationRepository.save(
                new SupplyRelationEntity(
                        command.relationId(),
                        command.supplierOrganizationId(),
                        command.merchantOrganizationId(),
                        normalizedAuthorizedCatalogScope,
                        command.supplyPriceRule(),
                        command.settlementRule(),
                        SupplyRelationStatus.PENDING,
                        command.operatorId(),
                        command.operationReason(),
                        OffsetDateTime.now(),
                        1L
                )
        );
        return toView(relation);
    }

    @Transactional
    public FoundationQueryModels.SupplyRelationView approveSupplyRelation(String relationId,
                                                                          FoundationOperationCommand command) {
        SupplyRelationEntity relation = supplyRelationRepository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("供货关系不存在: " + relationId));
        relation.changeStatus(
                SupplyRelationStatus.ACTIVE,
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        supplyRelationRepository.save(relation);
        foundationEventAppender.append(
                "SupplyRelation",
                relationId,
                "SupplyRelationActivated",
                relation.getAggregateVersion(),
                new FoundationEventPayload(
                        relationId,
                        null,
                        null,
                        relation.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                )
        );
        return toView(relation);
    }

    @Transactional
    public FoundationQueryModels.SupplyRelationView restoreSupplyRelation(String relationId,
                                                                          FoundationOperationCommand command) {
        return approveSupplyRelation(relationId, command);
    }

    @Transactional
    public FoundationQueryModels.SupplyRelationView suspendSupplyRelation(String relationId,
                                                                          FoundationOperationCommand command) {
        SupplyRelationEntity relation = supplyRelationRepository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("供货关系不存在: " + relationId));
        relation.changeStatus(
                SupplyRelationStatus.SUSPENDED,
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        supplyRelationRepository.save(relation);
        foundationEventAppender.append(
                "SupplyRelation",
                relationId,
                "SupplyRelationDeactivated",
                relation.getAggregateVersion(),
                new FoundationEventPayload(
                        relationId,
                        null,
                        null,
                        relation.getStatus().name(),
                        command.operatorId(),
                        command.operationReason()
                )
        );
        return toView(relation);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveQualification(String organizationId) {
        return qualificationRepository.findByOrganizationId(organizationId)
                .filter(entity -> entity.getStatus() == DirectQualificationStatus.ACTIVE)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.SupplyRelationResolutionView resolveSupplyRelation(String supplierOrganizationId,
                                                                                    String merchantOrganizationId) {
        return resolveSupplyRelation(supplierOrganizationId, merchantOrganizationId, null, null);
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.SupplyRelationResolutionView resolveSupplyRelation(String supplierOrganizationId,
                                                                                    String merchantOrganizationId,
                                                                                    String categoryId,
                                                                                    String productId) {
        SupplyRelationEntity relation = supplyRelationRepository
                .findBySupplierOrganizationIdAndMerchantOrganizationId(supplierOrganizationId, merchantOrganizationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "供货关系不存在: " + supplierOrganizationId + " -> " + merchantOrganizationId
                ));
        String resolvedAuthorizedCatalogScope = relation.getAuthorizedCatalogScope();
        String normalizedAuthorizedCatalogScope = null;
        CatalogAuthorizationDecision authorizationDecision;
        try {
            AuthorizedCatalogScope authorizedCatalogScope = AuthorizedCatalogScope.parse(relation.getAuthorizedCatalogScope());
            normalizedAuthorizedCatalogScope = authorizedCatalogScope.normalizedValue();
            resolvedAuthorizedCatalogScope = normalizedAuthorizedCatalogScope;
            authorizationDecision = authorizedCatalogScope.authorize(categoryId, productId);
        } catch (IllegalArgumentException ex) {
            authorizationDecision = hasAuthorizationTarget(categoryId, productId)
                    ? CatalogAuthorizationDecision.rejected("INVALID_SCOPE_CONFIGURED")
                    : CatalogAuthorizationDecision.notEvaluated("INVALID_SCOPE_CONFIGURED");
        }
        if (authorizationDecision.catalogAuthorized() != null && relation.getStatus() != SupplyRelationStatus.ACTIVE) {
            authorizationDecision = CatalogAuthorizationDecision.rejected("SUPPLY_RELATION_INACTIVE");
        }
        return new FoundationQueryModels.SupplyRelationResolutionView(
                relation.getRelationId(),
                relation.getSupplierOrganizationId(),
                relation.getMerchantOrganizationId(),
                resolvedAuthorizedCatalogScope,
                normalizedAuthorizedCatalogScope,
                relation.getSupplyPriceRule(),
                relation.getSettlementRule(),
                relation.getStatus().name(),
                relation.getStatus() == SupplyRelationStatus.ACTIVE,
                authorizationDecision.catalogAuthorized(),
                authorizationDecision.matchedScopeType(),
                authorizationDecision.matchedScopeValue(),
                authorizationDecision.authorizationReason(),
                relation.getAggregateVersion()
        );
    }

    private boolean hasAuthorizationTarget(String categoryId, String productId) {
        return hasText(categoryId) || hasText(productId);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private FoundationQueryModels.DirectQualificationView toView(DirectSupplierQualificationEntity entity) {
        return new FoundationQueryModels.DirectQualificationView(
                entity.getQualificationId(),
                entity.getOrganizationId(),
                entity.getStatus().name(),
                entity.getAggregateVersion()
        );
    }

    private FoundationQueryModels.SupplyRelationView toView(SupplyRelationEntity entity) {
        return new FoundationQueryModels.SupplyRelationView(
                entity.getRelationId(),
                entity.getSupplierOrganizationId(),
                entity.getMerchantOrganizationId(),
                entity.getStatus().name(),
                entity.getAggregateVersion()
        );
    }
}
