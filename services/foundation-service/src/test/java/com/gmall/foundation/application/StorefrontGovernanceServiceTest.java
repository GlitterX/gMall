package com.gmall.foundation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmall.foundation.domain.model.StorefrontStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.persistence.StorefrontEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontPermissionBindingEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontPermissionBindingRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontTerminalEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontTerminalRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorefrontGovernanceServiceTest {

    private final StorefrontRepository storefrontRepository = mock(StorefrontRepository.class);
    private final StorefrontTerminalRepository storefrontTerminalRepository = mock(StorefrontTerminalRepository.class);
    private final StorefrontPermissionBindingRepository permissionBindingRepository = mock(StorefrontPermissionBindingRepository.class);
    private final FoundationEventAppender foundationEventAppender = mock(FoundationEventAppender.class);
    private final DecorationSnapshotGateway decorationSnapshotGateway = mock(DecorationSnapshotGateway.class);

    private final List<StorefrontTerminalEntity> savedTerminals = new ArrayList<>();
    private final List<StorefrontPermissionBindingEntity> savedBindings = new ArrayList<>();

    private StorefrontGovernanceService storefrontGovernanceService;

    @BeforeEach
    void setUp() {
        storefrontGovernanceService = new StorefrontGovernanceService(
                storefrontRepository,
                storefrontTerminalRepository,
                permissionBindingRepository,
                foundationEventAppender,
                decorationSnapshotGateway
        );
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
    void updateTerminalStatusRequiresActiveStorefrontAndAllowsEnablement() {
        StorefrontEntity pendingStorefront = new StorefrontEntity(
                "store-1",
                "org-1",
                "seller-1",
                "MERCHANT",
                StorefrontStatus.PENDING,
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "create",
                OffsetDateTime.parse("2026-03-31T10:00:00+08:00"),
                1L
        );
        when(storefrontRepository.findById("store-1")).thenReturn(Optional.of(pendingStorefront));

        assertThatThrownBy(() -> storefrontGovernanceService.updateTerminal(
                "store-1",
                "MOBILE",
                new StorefrontTerminalCommand(true, "tester", "enable")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("店铺未激活，禁止启用终端");

        StorefrontEntity activeStorefront = new StorefrontEntity(
                "store-1",
                "org-1",
                "seller-1",
                "MERCHANT",
                StorefrontStatus.ACTIVE,
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "create",
                OffsetDateTime.parse("2026-03-31T10:00:00+08:00"),
                2L
        );
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-1",
                "MOBILE",
                false,
                null,
                "tester",
                "init",
                OffsetDateTime.parse("2026-03-31T10:00:00+08:00"),
                1L
        );
        when(storefrontRepository.findById("store-1")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-1::MOBILE")).thenReturn(Optional.of(terminal));

        FoundationQueryModels.StorefrontTerminalView terminalView = storefrontGovernanceService.updateTerminal(
                "store-1",
                "MOBILE",
                new StorefrontTerminalCommand(true, "tester", "enable")
        );
        storefrontGovernanceService.updatePermissionBinding(
                "store-1",
                new StorefrontPermissionBindingCommand("role-editor", "role-submitter", "tester", "bind")
        );

        assertThat(terminalView.enabled()).isTrue();
        assertThat(terminalView.homePageId()).isNull();
        assertThat(savedTerminals).singleElement().satisfies(entity -> {
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.getHomePageId()).isNull();
        });
        assertThat(savedBindings).singleElement().satisfies(entity -> {
            assertThat(entity.getEditorRoleIds()).isEqualTo("role-editor");
            assertThat(entity.getSubmitterRoleIds()).isEqualTo("role-submitter");
        });
        verify(foundationEventAppender).append(
                org.mockito.ArgumentMatchers.eq("StorefrontTerminal"),
                org.mockito.ArgumentMatchers.eq("store-1::MOBILE"),
                org.mockito.ArgumentMatchers.eq("StorefrontTerminalChanged"),
                org.mockito.ArgumentMatchers.eq(2L),
                any()
        );
    }

    @Test
    void bindHomePageRequiresEnabledTerminal() {
        StorefrontEntity activeStorefront = new StorefrontEntity(
                "store-1",
                "org-1",
                "seller-1",
                "MERCHANT",
                StorefrontStatus.ACTIVE,
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "create",
                OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                2L
        );
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-1",
                "MOBILE",
                false,
                null,
                "tester",
                "disable",
                OffsetDateTime.parse("2026-04-01T10:35:00+08:00"),
                1L
        );
        when(storefrontRepository.findById("store-1")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-1::MOBILE")).thenReturn(Optional.of(terminal));

        assertThatThrownBy(() -> storefrontGovernanceService.bindHomePage(
                "store-1",
                "MOBILE",
                new StorefrontHomePageBindingCommand("home-mobile", "tester", "bind-home")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("终端未启用，禁止绑定首页");
    }

    @Test
    void bindHomePageRejectsUnreadableSnapshot() {
        StorefrontEntity activeStorefront = createActiveStorefront("store-1");
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-1",
                "MOBILE",
                true,
                null,
                "tester",
                "enable",
                OffsetDateTime.parse("2026-04-01T10:35:00+08:00"),
                1L
        );
        when(storefrontRepository.findById("store-1")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-1::MOBILE")).thenReturn(Optional.of(terminal));
        when(decorationSnapshotGateway.verifyHomePageSnapshot("store-1", "home-mobile", "MOBILE"))
                .thenReturn(snapshotVerification(HomePageValidationStatus.SNAPSHOT_UNREADABLE));

        assertThatThrownBy(() -> storefrontGovernanceService.bindHomePage(
                "store-1",
                "MOBILE",
                new StorefrontHomePageBindingCommand("home-mobile", "tester", "bind-home")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("首页页面不存在可读发布快照或不归属当前店铺");
    }

    @Test
    void bindHomePagePersistsWhenSnapshotReadable() {
        StorefrontEntity activeStorefront = createActiveStorefront("store-1");
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-1",
                "MOBILE",
                true,
                null,
                "tester",
                "enable",
                OffsetDateTime.parse("2026-04-01T10:35:00+08:00"),
                1L
        );
        when(storefrontRepository.findById("store-1")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-1::MOBILE")).thenReturn(Optional.of(terminal));
        when(decorationSnapshotGateway.verifyHomePageSnapshot("store-1", "home-mobile", "MOBILE"))
                .thenReturn(snapshotVerification(HomePageValidationStatus.VALID));

        FoundationQueryModels.StorefrontTerminalView terminalView = storefrontGovernanceService.bindHomePage(
                "store-1",
                "MOBILE",
                new StorefrontHomePageBindingCommand("home-mobile", "tester", "bind-home")
        );

        assertThat(terminalView.homePageId()).isEqualTo("home-mobile");
        assertThat(savedTerminals).singleElement().satisfies(entity -> {
            assertThat(entity.getHomePageId()).isEqualTo("home-mobile");
            assertThat(entity.isEnabled()).isTrue();
        });
    }

    @Test
    void getOperabilitySummarizesStorefrontTerminalPermissionBindingAndHomePageValidation() {
        StorefrontEntity activeStorefront = new StorefrontEntity(
                "store-2",
                "org-2",
                "seller-2",
                "MERCHANT",
                StorefrontStatus.ACTIVE,
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "create",
                OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                2L
        );
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-2",
                "MOBILE",
                true,
                "home-mobile",
                "tester",
                "enable",
                OffsetDateTime.parse("2026-04-01T10:35:00+08:00"),
                3L
        );
        StorefrontPermissionBindingEntity binding = new StorefrontPermissionBindingEntity(
                "store-2",
                "role-editor",
                "role-submitter",
                2L,
                "tester",
                "bind",
                OffsetDateTime.parse("2026-04-01T10:40:00+08:00")
        );
        when(storefrontRepository.findById("store-2")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-2::MOBILE")).thenReturn(Optional.of(terminal));
        when(permissionBindingRepository.findById("store-2")).thenReturn(Optional.of(binding));
        when(decorationSnapshotGateway.verifyHomePageSnapshot("store-2", "home-mobile", "MOBILE"))
                .thenReturn(snapshotVerification(HomePageValidationStatus.VALID));

        FoundationQueryModels.StorefrontOperabilityView operabilityView =
                storefrontGovernanceService.getOperability("store-2", "MOBILE");

        assertThat(operabilityView.storefrontActive()).isTrue();
        assertThat(operabilityView.permissionBindingReady()).isTrue();
        assertThat(operabilityView.terminalEnabled()).isTrue();
        assertThat(operabilityView.homePageBound()).isTrue();
        assertThat(operabilityView.homePageValidationStatus()).isEqualTo("VALID");
        assertThat(operabilityView.homePageValidationPassed()).isTrue();
        assertThat(operabilityView.operable()).isTrue();
        assertThat(operabilityView.bindingVersion()).isEqualTo(2L);
        assertThat(operabilityView.allowed()).isTrue();
    }

    @Test
    void getOperabilityReturnsNotBoundWhenNoHomePageIsConfigured() {
        StorefrontEntity activeStorefront = createActiveStorefront("store-3");
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-3",
                "PC",
                true,
                null,
                "tester",
                "enable",
                OffsetDateTime.parse("2026-04-01T11:05:00+08:00"),
                1L
        );
        StorefrontPermissionBindingEntity binding = new StorefrontPermissionBindingEntity(
                "store-3",
                "role-editor",
                "",
                1L,
                "tester",
                "bind",
                OffsetDateTime.parse("2026-04-01T11:10:00+08:00")
        );
        when(storefrontRepository.findById("store-3")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-3::PC")).thenReturn(Optional.of(terminal));
        when(permissionBindingRepository.findById("store-3")).thenReturn(Optional.of(binding));

        FoundationQueryModels.StorefrontOperabilityView operabilityView =
                storefrontGovernanceService.getOperability("store-3", "PC");

        assertThat(operabilityView.operable()).isTrue();
        assertThat(operabilityView.homePageBound()).isFalse();
        assertThat(operabilityView.homePageValidationStatus()).isEqualTo("NOT_BOUND");
        assertThat(operabilityView.homePageValidationPassed()).isFalse();
        assertThat(operabilityView.allowed()).isTrue();
    }

    @Test
    void getOperabilityReturnsUnreadableWhenSnapshotValidationFails() {
        StorefrontEntity activeStorefront = createActiveStorefront("store-4");
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-4",
                "PC",
                true,
                "page-pc",
                "tester",
                "enable",
                OffsetDateTime.parse("2026-04-01T11:05:00+08:00"),
                1L
        );
        StorefrontPermissionBindingEntity binding = new StorefrontPermissionBindingEntity(
                "store-4",
                "role-editor",
                "",
                1L,
                "tester",
                "bind",
                OffsetDateTime.parse("2026-04-01T11:10:00+08:00")
        );
        when(storefrontRepository.findById("store-4")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-4::PC")).thenReturn(Optional.of(terminal));
        when(permissionBindingRepository.findById("store-4")).thenReturn(Optional.of(binding));
        when(decorationSnapshotGateway.verifyHomePageSnapshot("store-4", "page-pc", "PC"))
                .thenReturn(snapshotVerification(HomePageValidationStatus.SNAPSHOT_UNREADABLE));

        FoundationQueryModels.StorefrontOperabilityView operabilityView =
                storefrontGovernanceService.getOperability("store-4", "PC");

        assertThat(operabilityView.operable()).isTrue();
        assertThat(operabilityView.homePageValidationStatus()).isEqualTo("SNAPSHOT_UNREADABLE");
        assertThat(operabilityView.homePageValidationPassed()).isFalse();
        assertThat(operabilityView.allowed()).isTrue();
    }

    @Test
    void getOperabilityRejectsPublishWhenBoundHomePageValidationFails() {
        StorefrontEntity activeStorefront = createActiveStorefront("store-4b");
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-4b",
                "PC",
                true,
                "page-pc",
                "tester",
                "enable",
                OffsetDateTime.parse("2026-04-01T11:05:00+08:00"),
                1L
        );
        StorefrontPermissionBindingEntity binding = new StorefrontPermissionBindingEntity(
                "store-4b",
                "role-editor",
                "",
                1L,
                "tester",
                "bind",
                OffsetDateTime.parse("2026-04-01T11:10:00+08:00")
        );
        when(storefrontRepository.findById("store-4b")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-4b::PC")).thenReturn(Optional.of(terminal));
        when(permissionBindingRepository.findById("store-4b")).thenReturn(Optional.of(binding));
        when(decorationSnapshotGateway.verifyHomePageSnapshot("store-4b", "page-pc", "PC"))
                .thenReturn(snapshotVerification(HomePageValidationStatus.SNAPSHOT_UNREADABLE));

        FoundationQueryModels.StorefrontOperabilityView operabilityView =
                storefrontGovernanceService.getOperability("store-4b", "PC", "PUBLISH");

        assertThat(operabilityView.homePageValidationStatus()).isEqualTo("SNAPSHOT_UNREADABLE");
        assertThat(operabilityView.allowed()).isFalse();
    }

    @Test
    void getOperabilityReturnsUnavailableWhenRemoteValidationCannotBeRead() {
        StorefrontEntity activeStorefront = createActiveStorefront("store-5");
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-5",
                "PC",
                true,
                "page-pc",
                "tester",
                "enable",
                OffsetDateTime.parse("2026-04-01T11:05:00+08:00"),
                1L
        );
        StorefrontPermissionBindingEntity binding = new StorefrontPermissionBindingEntity(
                "store-5",
                "role-editor",
                "",
                1L,
                "tester",
                "bind",
                OffsetDateTime.parse("2026-04-01T11:10:00+08:00")
        );
        when(storefrontRepository.findById("store-5")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-5::PC")).thenReturn(Optional.of(terminal));
        when(permissionBindingRepository.findById("store-5")).thenReturn(Optional.of(binding));
        when(decorationSnapshotGateway.verifyHomePageSnapshot("store-5", "page-pc", "PC"))
                .thenReturn(snapshotVerification(HomePageValidationStatus.VALIDATION_UNAVAILABLE));

        FoundationQueryModels.StorefrontOperabilityView operabilityView =
                storefrontGovernanceService.getOperability("store-5", "PC");

        assertThat(operabilityView.operable()).isTrue();
        assertThat(operabilityView.homePageValidationStatus()).isEqualTo("VALIDATION_UNAVAILABLE");
        assertThat(operabilityView.homePageValidationPassed()).isFalse();
        assertThat(operabilityView.allowed()).isTrue();
    }

    @Test
    void getOperabilityRequiresEnabledTerminalForTerminalScopedOperation() {
        StorefrontEntity activeStorefront = new StorefrontEntity(
                "store-6",
                "org-6",
                "seller-6",
                "MERCHANT",
                StorefrontStatus.ACTIVE,
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "create",
                OffsetDateTime.parse("2026-04-01T11:00:00+08:00"),
                2L
        );
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-6",
                "PC",
                false,
                null,
                "tester",
                "disable",
                OffsetDateTime.parse("2026-04-01T11:05:00+08:00"),
                1L
        );
        StorefrontPermissionBindingEntity binding = new StorefrontPermissionBindingEntity(
                "store-6",
                "role-editor",
                "",
                1L,
                "tester",
                "bind",
                OffsetDateTime.parse("2026-04-01T11:10:00+08:00")
        );
        when(storefrontRepository.findById("store-6")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-6::PC")).thenReturn(Optional.of(terminal));
        when(permissionBindingRepository.findById("store-6")).thenReturn(Optional.of(binding));

        FoundationQueryModels.StorefrontOperabilityView operabilityView =
                storefrontGovernanceService.getOperability("store-6", "PC");

        assertThat(operabilityView.storefrontActive()).isTrue();
        assertThat(operabilityView.permissionBindingReady()).isTrue();
        assertThat(operabilityView.terminalEnabled()).isFalse();
        assertThat(operabilityView.operable()).isFalse();
        assertThat(operabilityView.homePageValidationStatus()).isEqualTo("NOT_BOUND");
        assertThat(operabilityView.allowed()).isFalse();
    }

    @Test
    void getOperabilityResolvesAllowedForOperationParameter() {
        StorefrontEntity activeStorefront = createActiveStorefront("store-7");
        StorefrontTerminalEntity terminal = new StorefrontTerminalEntity(
                "store-7",
                "MOBILE",
                true,
                null,
                "tester",
                "enable",
                OffsetDateTime.parse("2026-04-01T11:05:00+08:00"),
                1L
        );
        StorefrontPermissionBindingEntity binding = new StorefrontPermissionBindingEntity(
                "store-7",
                "role-editor",
                "role-submitter",
                1L,
                "tester",
                "bind",
                OffsetDateTime.parse("2026-04-01T11:10:00+08:00")
        );
        when(storefrontRepository.findById("store-7")).thenReturn(Optional.of(activeStorefront));
        when(storefrontTerminalRepository.findById("store-7::MOBILE")).thenReturn(Optional.of(terminal));
        when(permissionBindingRepository.findById("store-7")).thenReturn(Optional.of(binding));

        FoundationQueryModels.StorefrontOperabilityView operabilityView =
                storefrontGovernanceService.getOperability("store-7", "MOBILE", "PUBLISH");

        assertThat(operabilityView.allowed()).isTrue();
    }

    private StorefrontEntity createActiveStorefront(String storefrontId) {
        return new StorefrontEntity(
                storefrontId,
                "org-" + storefrontId,
                "seller-" + storefrontId,
                "MERCHANT",
                StorefrontStatus.ACTIVE,
                "zh-CN",
                "zh-CN,en-US",
                "tester",
                "create",
                OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                2L
        );
    }

    private DecorationSnapshotGateway.HomePageSnapshotVerification snapshotVerification(HomePageValidationStatus status) {
        return new DecorationSnapshotGateway.HomePageSnapshotVerification(
                status,
                status == HomePageValidationStatus.VALID ? "CHECKSUM_MATCH" : null,
                status == HomePageValidationStatus.VALID ? "snapshot-1" : null,
                status == HomePageValidationStatus.VALID ? 1 : null,
                status == HomePageValidationStatus.VALID ? "checksum-1" : null,
                status == HomePageValidationStatus.VALID
        );
    }
}
