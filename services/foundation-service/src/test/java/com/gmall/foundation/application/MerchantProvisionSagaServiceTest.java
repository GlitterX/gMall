package com.gmall.foundation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.messaging.FoundationEventPayload;
import com.gmall.foundation.infrastructure.persistence.DirectSupplierQualificationEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MerchantProvisionSagaServiceTest {

    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final SellerRepository sellerRepository = mock(SellerRepository.class);
    private final StorefrontRepository storefrontRepository = mock(StorefrontRepository.class);
    private final DirectSupplierQualificationRepository qualificationRepository = mock(DirectSupplierQualificationRepository.class);
    private final StorefrontTerminalRepository storefrontTerminalRepository = mock(StorefrontTerminalRepository.class);
    private final StorefrontPermissionBindingRepository permissionBindingRepository = mock(StorefrontPermissionBindingRepository.class);
    private final FoundationEventAppender foundationEventAppender = mock(FoundationEventAppender.class);
    private final MerchantProvisionSagaHook merchantProvisionSagaHook = mock(MerchantProvisionSagaHook.class);

    private final List<OrganizationEntity> savedOrganizations = new ArrayList<>();
    private final List<SellerEntity> savedSellers = new ArrayList<>();
    private final List<StorefrontEntity> savedStorefronts = new ArrayList<>();
    private final List<StorefrontTerminalEntity> savedTerminals = new ArrayList<>();
    private final List<StorefrontPermissionBindingEntity> savedBindings = new ArrayList<>();

    private MerchantProvisionSagaService merchantProvisionSagaService;

    @BeforeEach
    void setUp() {
        merchantProvisionSagaService = new MerchantProvisionSagaService(
                organizationRepository,
                sellerRepository,
                storefrontRepository,
                qualificationRepository,
                storefrontTerminalRepository,
                permissionBindingRepository,
                foundationEventAppender,
                merchantProvisionSagaHook
        );
        when(organizationRepository.save(any(OrganizationEntity.class)))
                .thenAnswer(invocation -> {
                    OrganizationEntity entity = invocation.getArgument(0);
                    savedOrganizations.add(entity);
                    return entity;
                });
        when(organizationRepository.findById(any())).thenReturn(Optional.empty());
        when(sellerRepository.save(any(SellerEntity.class)))
                .thenAnswer(invocation -> {
                    SellerEntity entity = invocation.getArgument(0);
                    savedSellers.add(entity);
                    return entity;
                });
        when(storefrontRepository.save(any(StorefrontEntity.class)))
                .thenAnswer(invocation -> {
                    StorefrontEntity entity = invocation.getArgument(0);
                    savedStorefronts.add(entity);
                    return entity;
                });
        when(storefrontTerminalRepository.save(any(StorefrontTerminalEntity.class)))
                .thenAnswer(invocation -> {
                    StorefrontTerminalEntity entity = invocation.getArgument(0);
                    savedTerminals.add(entity);
                    return entity;
                });
        when(permissionBindingRepository.save(any(StorefrontPermissionBindingEntity.class)))
                .thenAnswer(invocation -> {
                    StorefrontPermissionBindingEntity entity = invocation.getArgument(0);
                    savedBindings.add(entity);
                    return entity;
                });
    }

    @Test
    void provisionPersistsProfilesAndAppendsThreeActivationEvents() {
        MerchantProvisionResult result = merchantProvisionSagaService.provision(new MerchantProvisionCommand(
                "org-1",
                "seller-1",
                "store-1",
                "MERCHANT",
                "MERCHANT",
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "provision"
        ));

        assertThat(result.organizationId()).isEqualTo("org-1");
        assertThat(result.sellerId()).isEqualTo("seller-1");
        assertThat(result.storefrontId()).isEqualTo("store-1");
        assertThat(savedOrganizations).singleElement().satisfies(entity -> {
            assertThat(entity.getOrganizationId()).isEqualTo("org-1");
            assertThat(entity.getStatus().name()).isEqualTo("ACTIVE");
        });
        assertThat(savedSellers).singleElement().satisfies(entity -> {
            assertThat(entity.getSellerId()).isEqualTo("seller-1");
            assertThat(entity.getOrganizationId()).isEqualTo("org-1");
            assertThat(entity.getStatus().name()).isEqualTo("ACTIVE");
        });
        assertThat(savedStorefronts).singleElement().satisfies(entity -> {
            assertThat(entity.getStorefrontId()).isEqualTo("store-1");
            assertThat(entity.getSellerId()).isEqualTo("seller-1");
            assertThat(entity.getStatus().name()).isEqualTo("ACTIVE");
        });
        assertThat(savedTerminals)
                .extracting(StorefrontTerminalEntity::getTerminalType, StorefrontTerminalEntity::isEnabled)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("MOBILE", false),
                        org.assertj.core.groups.Tuple.tuple("PC", false)
                );
        assertThat(savedBindings).singleElement().satisfies(entity -> {
            assertThat(entity.getEditorRoleIds()).isEmpty();
            assertThat(entity.getSubmitterRoleIds()).isEmpty();
        });

        ArgumentCaptor<String> aggregateTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggregateIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> versionCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<FoundationEventPayload> payloadCaptor = ArgumentCaptor.forClass(FoundationEventPayload.class);
        verify(foundationEventAppender, org.mockito.Mockito.times(3)).append(
                aggregateTypeCaptor.capture(),
                aggregateIdCaptor.capture(),
                eventTypeCaptor.capture(),
                versionCaptor.capture(),
                payloadCaptor.capture()
        );

        assertThat(aggregateTypeCaptor.getAllValues()).containsExactly("Organization", "Seller", "Storefront");
        assertThat(aggregateIdCaptor.getAllValues()).containsExactly("org-1", "seller-1", "store-1");
        assertThat(eventTypeCaptor.getAllValues())
                .containsExactly("OrganizationActivated", "SellerActivated", "StorefrontActivated");
        assertThat(versionCaptor.getAllValues()).containsExactly(1L, 1L, 1L);
        assertThat(payloadCaptor.getAllValues())
                .extracting(FoundationEventPayload::organizationId, FoundationEventPayload::sellerId,
                        FoundationEventPayload::storefrontId, FoundationEventPayload::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("org-1", null, null, "ACTIVE"),
                        org.assertj.core.groups.Tuple.tuple("org-1", "seller-1", null, "ACTIVE"),
                        org.assertj.core.groups.Tuple.tuple("org-1", "seller-1", "store-1", "ACTIVE")
                );
    }

    @Test
    void provisionDirectSupplierRequiresActiveQualification() {
        when(qualificationRepository.findByOrganizationId("org-supplier")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> merchantProvisionSagaService.provision(new MerchantProvisionCommand(
                "org-supplier",
                "seller-supplier",
                "store-supplier",
                "SUPPLIER",
                "DIRECT_SUPPLIER",
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "provision"
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessage("直营供应商缺少有效资格");
    }
}
