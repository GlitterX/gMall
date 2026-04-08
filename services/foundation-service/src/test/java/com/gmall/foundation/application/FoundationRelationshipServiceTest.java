package com.gmall.foundation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmall.foundation.domain.model.DirectQualificationStatus;
import com.gmall.foundation.domain.model.SupplyRelationStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationEntity;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationRepository;
import com.gmall.foundation.infrastructure.persistence.SupplyRelationEntity;
import com.gmall.foundation.infrastructure.persistence.SupplyRelationRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FoundationRelationshipServiceTest {

    private final DirectSupplierQualificationRepository qualificationRepository = mock(DirectSupplierQualificationRepository.class);
    private final SupplyRelationRepository supplyRelationRepository = mock(SupplyRelationRepository.class);
    private final FoundationEventAppender foundationEventAppender = mock(FoundationEventAppender.class);

    private final List<DirectSupplierQualificationEntity> savedQualifications = new ArrayList<>();
    private final List<SupplyRelationEntity> savedRelations = new ArrayList<>();

    private FoundationRelationshipService foundationRelationshipService;

    @BeforeEach
    void setUp() {
        foundationRelationshipService = new FoundationRelationshipService(
                qualificationRepository,
                supplyRelationRepository,
                foundationEventAppender
        );
        when(qualificationRepository.save(any(DirectSupplierQualificationEntity.class)))
                .thenAnswer(invocation -> {
                    DirectSupplierQualificationEntity entity = invocation.getArgument(0);
                    savedQualifications.add(entity);
                    return entity;
                });
        when(supplyRelationRepository.save(any(SupplyRelationEntity.class)))
                .thenAnswer(invocation -> {
                    SupplyRelationEntity entity = invocation.getArgument(0);
                    savedRelations.add(entity);
                    return entity;
                });
    }

    @Test
    void approveQualificationAndRestoreSupplyRelation() {
        DirectSupplierQualificationEntity qualification = new DirectSupplierQualificationEntity(
                "qual-1",
                "org-supplier",
                DirectQualificationStatus.PENDING,
                "tester",
                "create",
                OffsetDateTime.parse("2026-03-31T10:00:00+08:00"),
                1L
        );
        SupplyRelationEntity relation = new SupplyRelationEntity(
                "rel-1",
                "org-supplier",
                "org-merchant",
                "ALL",
                "PRICE_RULE",
                "SETTLEMENT_RULE",
                SupplyRelationStatus.SUSPENDED,
                "tester",
                "suspend",
                OffsetDateTime.parse("2026-03-31T10:00:00+08:00"),
                2L
        );
        when(qualificationRepository.findById("qual-1")).thenReturn(Optional.of(qualification));
        when(supplyRelationRepository.findById("rel-1")).thenReturn(Optional.of(relation));

        FoundationQueryModels.DirectQualificationView qualificationView = foundationRelationshipService.approveQualification(
                "qual-1",
                new FoundationOperationCommand("reviewer", "approve")
        );
        FoundationQueryModels.SupplyRelationView relationView = foundationRelationshipService.restoreSupplyRelation(
                "rel-1",
                new FoundationOperationCommand("reviewer", "restore")
        );

        assertThat(qualificationView.status()).isEqualTo("ACTIVE");
        assertThat(savedQualifications).singleElement().satisfies(entity -> {
            assertThat(entity.getStatus()).isEqualTo(DirectQualificationStatus.ACTIVE);
            assertThat(entity.getAggregateVersion()).isEqualTo(2L);
        });
        assertThat(relationView.status()).isEqualTo("ACTIVE");
        assertThat(savedRelations).singleElement().satisfies(entity -> {
            assertThat(entity.getStatus()).isEqualTo(SupplyRelationStatus.ACTIVE);
            assertThat(entity.getAggregateVersion()).isEqualTo(3L);
        });
        verify(foundationEventAppender).append(
                org.mockito.ArgumentMatchers.eq("SupplyRelation"),
                org.mockito.ArgumentMatchers.eq("rel-1"),
                org.mockito.ArgumentMatchers.eq("SupplyRelationActivated"),
                org.mockito.ArgumentMatchers.eq(3L),
                any()
        );
    }

    @Test
    void resolveSupplyRelationReturnsCatalogAndSettlementFacts() {
        SupplyRelationEntity relation = new SupplyRelationEntity(
                "rel-2",
                "org-supplier",
                "org-merchant",
                "CATALOG:ALL",
                "PRICE_RULE:STANDARD",
                "SETTLEMENT_RULE:MERCHANT",
                SupplyRelationStatus.ACTIVE,
                "tester",
                "approve",
                OffsetDateTime.parse("2026-04-01T10:00:00+08:00"),
                4L
        );
        when(supplyRelationRepository.findBySupplierOrganizationIdAndMerchantOrganizationId("org-supplier", "org-merchant"))
                .thenReturn(Optional.of(relation));

        FoundationQueryModels.SupplyRelationResolutionView resolutionView =
                foundationRelationshipService.resolveSupplyRelation("org-supplier", "org-merchant", null, "prod-9");

        assertThat(resolutionView.relationId()).isEqualTo("rel-2");
        assertThat(resolutionView.authorizedCatalogScope()).isEqualTo("CATALOG:ALL");
        assertThat(resolutionView.normalizedAuthorizedCatalogScope()).isEqualTo("CATALOG:ALL");
        assertThat(resolutionView.supplyPriceRule()).isEqualTo("PRICE_RULE:STANDARD");
        assertThat(resolutionView.settlementRule()).isEqualTo("SETTLEMENT_RULE:MERCHANT");
        assertThat(resolutionView.active()).isTrue();
        assertThat(resolutionView.catalogAuthorized()).isTrue();
        assertThat(resolutionView.matchedScopeType()).isEqualTo("CATALOG");
        assertThat(resolutionView.authorizationReason()).isEqualTo("CATALOG_ALL_AUTHORIZED");
        assertThat(resolutionView.aggregateVersion()).isEqualTo(4L);
    }

    @Test
    void createSupplyRelationNormalizesLegacyScope() {
        foundationRelationshipService.createSupplyRelation(new SupplyRelationCommand(
                "rel-3",
                "org-supplier",
                "org-merchant",
                "ALL",
                "PRICE_RULE:STANDARD",
                "SETTLEMENT_RULE:MERCHANT",
                "tester",
                "create"
        ));

        assertThat(savedRelations).singleElement().satisfies(entity -> {
            assertThat(entity.getAuthorizedCatalogScope()).isEqualTo("CATALOG:ALL");
            assertThat(entity.getStatus()).isEqualTo(SupplyRelationStatus.PENDING);
        });
    }

    @Test
    void createSupplyRelationRejectsInvalidScope() {
        assertThatThrownBy(() -> foundationRelationshipService.createSupplyRelation(new SupplyRelationCommand(
                "rel-4",
                "org-supplier",
                "org-merchant",
                "BRAND:brand-1",
                "PRICE_RULE:STANDARD",
                "SETTLEMENT_RULE:MERCHANT",
                "tester",
                "create"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的授权范围类型");
    }

    @Test
    void resolveSupplyRelationRejectsInactiveRelationEvenWhenScopeMatches() {
        SupplyRelationEntity relation = new SupplyRelationEntity(
                "rel-5",
                "org-supplier",
                "org-merchant",
                "PRODUCT:prod-1",
                "PRICE_RULE:STANDARD",
                "SETTLEMENT_RULE:MERCHANT",
                SupplyRelationStatus.SUSPENDED,
                "tester",
                "suspend",
                OffsetDateTime.parse("2026-04-01T10:00:00+08:00"),
                2L
        );
        when(supplyRelationRepository.findBySupplierOrganizationIdAndMerchantOrganizationId("org-supplier", "org-merchant"))
                .thenReturn(Optional.of(relation));

        FoundationQueryModels.SupplyRelationResolutionView resolutionView =
                foundationRelationshipService.resolveSupplyRelation("org-supplier", "org-merchant", null, "prod-1");

        assertThat(resolutionView.active()).isFalse();
        assertThat(resolutionView.catalogAuthorized()).isFalse();
        assertThat(resolutionView.matchedScopeType()).isEqualTo("NONE");
        assertThat(resolutionView.authorizationReason()).isEqualTo("SUPPLY_RELATION_INACTIVE");
    }
}
