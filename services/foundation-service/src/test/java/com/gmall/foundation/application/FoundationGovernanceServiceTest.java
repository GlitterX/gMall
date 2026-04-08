package com.gmall.foundation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmall.foundation.domain.model.OrganizationStatus;
import com.gmall.foundation.domain.model.DirectQualificationStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationEntity;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationRepository;
import com.gmall.foundation.infrastructure.persistence.OrganizationEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationRepository;
import com.gmall.foundation.infrastructure.persistence.SellerRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FoundationGovernanceServiceTest {

    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final DirectSupplierQualificationRepository directSupplierQualificationRepository = mock(DirectSupplierQualificationRepository.class);
    private final SellerRepository sellerRepository = mock(SellerRepository.class);
    private final StorefrontRepository storefrontRepository = mock(StorefrontRepository.class);
    private final FoundationEventAppender foundationEventAppender = mock(FoundationEventAppender.class);

    private FoundationGovernanceService foundationGovernanceService;

    @BeforeEach
    void setUp() {
        foundationGovernanceService = new FoundationGovernanceService(
                organizationRepository,
                directSupplierQualificationRepository,
                sellerRepository,
                storefrontRepository,
                foundationEventAppender
        );
    }

    @Test
    void getOrganizationEligibilityRequiresActiveOrganization() {
        OrganizationEntity frozenOrganization = new OrganizationEntity(
                "org-1",
                "MERCHANT",
                "Merchant A",
                OrganizationStatus.FROZEN,
                "apply-1",
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "freeze",
                OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                3L
        );
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(frozenOrganization));

        FoundationQueryModels.OrganizationEligibilityView frozenView =
                foundationGovernanceService.getOrganizationEligibility("org-1");

        assertThat(frozenView.sellerActivationAllowed()).isFalse();
        assertThat(frozenView.storefrontProvisionAllowed()).isFalse();

        OrganizationEntity activeOrganization = new OrganizationEntity(
                "org-2",
                "MERCHANT",
                "Merchant B",
                OrganizationStatus.ACTIVE,
                "apply-2",
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "approve",
                OffsetDateTime.parse("2026-04-01T09:30:00+08:00"),
                5L
        );
        when(organizationRepository.findById("org-2")).thenReturn(Optional.of(activeOrganization));

        FoundationQueryModels.OrganizationEligibilityView activeView =
                foundationGovernanceService.getOrganizationEligibility("org-2");

        assertThat(activeView.sellerActivationAllowed()).isTrue();
        assertThat(activeView.storefrontProvisionAllowed()).isTrue();
        assertThat(activeView.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getOrganizationContextIncludesLocaleAndDirectQualificationState() {
        OrganizationEntity organization = new OrganizationEntity(
                "org-supplier-1",
                "SUPPLIER",
                "Supplier A",
                OrganizationStatus.ACTIVE,
                "apply-3",
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "approve",
                OffsetDateTime.parse("2026-04-01T10:00:00+08:00"),
                6L
        );
        when(organizationRepository.findById("org-supplier-1")).thenReturn(Optional.of(organization));
        when(directSupplierQualificationRepository.findByOrganizationId("org-supplier-1"))
                .thenReturn(Optional.of(new DirectSupplierQualificationEntity(
                        "qualification-1",
                        "org-supplier-1",
                        DirectQualificationStatus.ACTIVE,
                        "tester",
                        "approve",
                        OffsetDateTime.parse("2026-04-01T10:05:00+08:00"),
                        2L
                )));

        FoundationQueryModels.OrganizationContextView view =
                foundationGovernanceService.getOrganizationContext("org-supplier-1");

        assertThat(view.organizationId()).isEqualTo("org-supplier-1");
        assertThat(view.organizationType()).isEqualTo("SUPPLIER");
        assertThat(view.status()).isEqualTo("ACTIVE");
        assertThat(view.defaultLocale()).isEqualTo("zh-CN");
        assertThat(view.supportedLocales()).isEqualTo("zh-CN,en-US");
        assertThat(view.directSupplierQualified()).isTrue();
        assertThat(view.directSupplierQualificationStatus()).isEqualTo("ACTIVE");
        assertThat(view.aggregateVersion()).isEqualTo(6L);
    }
}
