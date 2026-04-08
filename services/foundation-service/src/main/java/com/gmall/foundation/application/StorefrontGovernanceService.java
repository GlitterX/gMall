package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.StorefrontStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.messaging.FoundationEventPayload;
import com.gmall.foundation.infrastructure.persistence.StorefrontEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontPermissionBindingEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontPermissionBindingRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontRepository;
import com.gmall.foundation.infrastructure.persistence.StorefrontTerminalEntity;
import com.gmall.foundation.infrastructure.persistence.StorefrontTerminalRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorefrontGovernanceService {

    private final StorefrontRepository storefrontRepository;
    private final StorefrontTerminalRepository storefrontTerminalRepository;
    private final StorefrontPermissionBindingRepository permissionBindingRepository;
    private final FoundationEventAppender foundationEventAppender;
    private final DecorationSnapshotGateway decorationSnapshotGateway;

    public StorefrontGovernanceService(StorefrontRepository storefrontRepository,
                                       StorefrontTerminalRepository storefrontTerminalRepository,
                                       StorefrontPermissionBindingRepository permissionBindingRepository,
                                       FoundationEventAppender foundationEventAppender,
                                       DecorationSnapshotGateway decorationSnapshotGateway) {
        this.storefrontRepository = storefrontRepository;
        this.storefrontTerminalRepository = storefrontTerminalRepository;
        this.permissionBindingRepository = permissionBindingRepository;
        this.foundationEventAppender = foundationEventAppender;
        this.decorationSnapshotGateway = decorationSnapshotGateway;
    }

    @Transactional
    public FoundationQueryModels.StorefrontTerminalView updateTerminal(String storefrontId,
                                                                       String terminalType,
                                                                       StorefrontTerminalCommand command) {
        StorefrontEntity storefront = storefrontRepository.findById(storefrontId)
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在: " + storefrontId));
        if (command.enabled() && storefront.getStatus() != StorefrontStatus.ACTIVE) {
            throw new IllegalStateException("店铺未激活，禁止启用终端");
        }
        StorefrontTerminalEntity terminal = storefrontTerminalRepository
                .findById(StorefrontTerminalEntity.buildKey(storefrontId, terminalType))
                .orElseGet(() -> new StorefrontTerminalEntity(
                        storefrontId,
                        terminalType,
                        false,
                        null,
                        command.operatorId(),
                        "init",
                        OffsetDateTime.now(),
                        0L
                ));
        terminal.update(
                command.enabled(),
                terminal.getHomePageId(),
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        storefrontTerminalRepository.save(terminal);
        foundationEventAppender.append(
                "StorefrontTerminal",
                terminal.getTerminalKey(),
                "StorefrontTerminalChanged",
                terminal.getAggregateVersion(),
                new FoundationEventPayload(
                        storefront.getOrganizationId(),
                        storefront.getSellerId(),
                        storefrontId,
                        command.enabled() ? "ENABLED" : "DISABLED",
                        command.operatorId(),
                        command.operationReason()
                )
        );
        return new FoundationQueryModels.StorefrontTerminalView(
                storefrontId,
                terminalType,
                terminal.isEnabled(),
                terminal.getHomePageId(),
                terminal.getAggregateVersion()
        );
    }

    @Transactional
    public FoundationQueryModels.StorefrontTerminalView bindHomePage(String storefrontId,
                                                                     String terminalType,
                                                                     StorefrontHomePageBindingCommand command) {
        storefrontRepository.findById(storefrontId)
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在: " + storefrontId));
        StorefrontTerminalEntity terminal = storefrontTerminalRepository
                .findById(StorefrontTerminalEntity.buildKey(storefrontId, terminalType))
                .orElseThrow(() -> new IllegalStateException("终端不存在，禁止绑定首页"));
        if (!terminal.isEnabled()) {
            throw new IllegalStateException("终端未启用，禁止绑定首页");
        }
        String homePageId = normalize(command.homePageId());
        if (homePageId == null) {
            terminal.update(
                    true,
                    null,
                    command.operatorId(),
                    command.operationReason(),
                    OffsetDateTime.now()
            );
            storefrontTerminalRepository.save(terminal);
            appendTerminalChangedEvent(storefrontId, terminal, "HOME_PAGE_UNBOUND", command.operatorId(), command.operationReason());
            return new FoundationQueryModels.StorefrontTerminalView(
                    storefrontId,
                    terminalType,
                    terminal.isEnabled(),
                    terminal.getHomePageId(),
                    terminal.getAggregateVersion()
            );
        }
        DecorationSnapshotGateway.HomePageSnapshotVerification verification = decorationSnapshotGateway
                .verifyHomePageSnapshot(storefrontId, homePageId, terminalType);
        HomePageValidationStatus status = verification.validationStatus();
        if (status == HomePageValidationStatus.SNAPSHOT_UNREADABLE) {
            throw new IllegalStateException("首页页面不存在可读发布快照或不归属当前店铺");
        }
        if (status == HomePageValidationStatus.VALIDATION_UNAVAILABLE) {
            throw new IllegalStateException("装修域快照校验暂不可用");
        }
        terminal.update(
                true,
                homePageId,
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        storefrontTerminalRepository.save(terminal);
        appendTerminalChangedEvent(storefrontId, terminal, "HOME_PAGE_BOUND", command.operatorId(), command.operationReason());
        return new FoundationQueryModels.StorefrontTerminalView(
                storefrontId,
                terminalType,
                terminal.isEnabled(),
                terminal.getHomePageId(),
                terminal.getAggregateVersion()
        );
    }

    @Transactional
    public void updatePermissionBinding(String storefrontId, StorefrontPermissionBindingCommand command) {
        StorefrontPermissionBindingEntity binding = permissionBindingRepository.findById(storefrontId)
                .orElseGet(() -> new StorefrontPermissionBindingEntity(
                        storefrontId,
                        "",
                        "",
                        0L,
                        command.operatorId(),
                        "init",
                        OffsetDateTime.now()
                ));
        binding.update(
                command.editorRoleIds(),
                command.submitterRoleIds(),
                command.operatorId(),
                command.operationReason(),
                OffsetDateTime.now()
        );
        permissionBindingRepository.save(binding);
        foundationEventAppender.append(
                "StorefrontPermissionBinding",
                storefrontId,
                "StorefrontPermissionBindingChanged",
                binding.getBindingVersion(),
                new FoundationEventPayload(
                        null,
                        null,
                        storefrontId,
                        "BOUND",
                        command.operatorId(),
                        command.operationReason()
                )
        );
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.StorefrontOperabilityView getOperability(String storefrontId, String terminalType) {
        return getOperability(storefrontId, terminalType, null);
    }

    @Transactional(readOnly = true)
    public FoundationQueryModels.StorefrontOperabilityView getOperability(String storefrontId,
                                                                          String terminalType,
                                                                          String operation) {
        StorefrontEntity storefront = storefrontRepository.findById(storefrontId)
                .orElseThrow(() -> new IllegalArgumentException("店铺不存在: " + storefrontId));
        StorefrontTerminalEntity terminal = storefrontTerminalRepository
                .findById(StorefrontTerminalEntity.buildKey(storefrontId, terminalType))
                .orElse(null);
        StorefrontPermissionBindingEntity binding = permissionBindingRepository.findById(storefrontId).orElse(null);
        boolean storefrontActive = storefront.getStatus() == StorefrontStatus.ACTIVE;
        boolean permissionBindingReady = binding != null
                && (hasText(binding.getEditorRoleIds()) || hasText(binding.getSubmitterRoleIds()));
        boolean terminalEnabled = terminal != null && terminal.isEnabled();
        boolean homePageBound = terminal != null && hasText(terminal.getHomePageId());
        boolean operable = storefrontActive && permissionBindingReady && terminalEnabled;
        HomePageValidationStatus homePageValidationStatus = resolveHomePageValidationStatus(storefrontId, terminalType, terminal);
        boolean allowed = resolveAllowed(operation, operable, homePageBound, homePageValidationStatus);
        return new FoundationQueryModels.StorefrontOperabilityView(
                storefrontId,
                terminalType,
                storefront.getStatus().name(),
                storefrontActive,
                permissionBindingReady,
                terminalEnabled,
                homePageBound,
                operable,
                homePageValidationStatus.name(),
                homePageValidationStatus == HomePageValidationStatus.VALID,
                storefront.getAggregateVersion(),
                terminal == null ? 0L : terminal.getAggregateVersion(),
                binding == null ? 0L : binding.getBindingVersion(),
                allowed
        );
    }

    private HomePageValidationStatus resolveHomePageValidationStatus(String storefrontId,
                                                                     String terminalType,
                                                                     StorefrontTerminalEntity terminal) {
        if (terminal == null || !hasText(terminal.getHomePageId())) {
            return HomePageValidationStatus.NOT_BOUND;
        }
        return decorationSnapshotGateway.verifyHomePageSnapshot(storefrontId, terminal.getHomePageId(), terminalType)
                .validationStatus();
    }

    private void appendTerminalChangedEvent(String storefrontId,
                                            StorefrontTerminalEntity terminal,
                                            String status,
                                            String operatorId,
                                            String operationReason) {
        foundationEventAppender.append(
                "StorefrontTerminal",
                terminal.getTerminalKey(),
                "StorefrontTerminalChanged",
                terminal.getAggregateVersion(),
                new FoundationEventPayload(
                        null,
                        null,
                        storefrontId,
                        status,
                        operatorId,
                        operationReason
                )
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean resolveAllowed(String operation,
                                   boolean operable,
                                   boolean homePageBound,
                                   HomePageValidationStatus homePageValidationStatus) {
        if (!operable) {
            return false;
        }
        if (!hasText(operation)) {
            return true;
        }
        return switch (operation.trim()) {
            case "EDIT", "SUBMIT" -> true;
            case "PUBLISH", "ROLLBACK" -> !homePageBound || homePageValidationStatus == HomePageValidationStatus.VALID;
            default -> throw new IllegalArgumentException("不支持的店铺操作类型: " + operation);
        };
    }

    private String normalize(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
