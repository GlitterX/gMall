package com.gmall.decoration.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.decoration.infrastructure.remote.DecorationRemoteException;
import com.gmall.decoration.infrastructure.messaging.DecorationEventAppender;
import com.gmall.decoration.infrastructure.persistence.DecorationDraftEntity;
import com.gmall.decoration.infrastructure.persistence.DecorationDraftRepository;
import com.gmall.decoration.infrastructure.persistence.DecorationPageEntity;
import com.gmall.decoration.infrastructure.persistence.DecorationPageRepository;
import com.gmall.decoration.infrastructure.persistence.DecorationPublishRecordEntity;
import com.gmall.decoration.infrastructure.persistence.DecorationPublishRecordRepository;
import com.gmall.decoration.infrastructure.persistence.DecorationReviewRecordEntity;
import com.gmall.decoration.infrastructure.persistence.DecorationReviewRecordRepository;
import com.gmall.decoration.infrastructure.persistence.DecorationSnapshotEntity;
import com.gmall.decoration.infrastructure.persistence.DecorationSnapshotRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecorationService {

    private static final List<String> WORKING_DRAFT_STATUSES = List.of("EDITING", "SUBMITTED", "APPROVED", "REJECTED");

    private final DecorationPageRepository pageRepository;
    private final DecorationDraftRepository draftRepository;
    private final DecorationReviewRecordRepository reviewRecordRepository;
    private final DecorationSnapshotRepository snapshotRepository;
    private final DecorationPublishRecordRepository publishRecordRepository;
    private final FoundationStorefrontGateway foundationStorefrontGateway;
    private final CatalogDecorationProjectionGateway catalogDecorationProjectionGateway;
    private final MarketingContentGateway marketingContentGateway;
    private final DecorationEventAppender decorationEventAppender;
    private final ObjectMapper objectMapper;

    public DecorationService(DecorationPageRepository pageRepository,
                             DecorationDraftRepository draftRepository,
                             DecorationReviewRecordRepository reviewRecordRepository,
                             DecorationSnapshotRepository snapshotRepository,
                             DecorationPublishRecordRepository publishRecordRepository,
                             FoundationStorefrontGateway foundationStorefrontGateway,
                             CatalogDecorationProjectionGateway catalogDecorationProjectionGateway,
                             MarketingContentGateway marketingContentGateway,
                             DecorationEventAppender decorationEventAppender,
                             ObjectMapper objectMapper) {
        this.pageRepository = pageRepository;
        this.draftRepository = draftRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.snapshotRepository = snapshotRepository;
        this.publishRecordRepository = publishRecordRepository;
        this.foundationStorefrontGateway = foundationStorefrontGateway;
        this.catalogDecorationProjectionGateway = catalogDecorationProjectionGateway;
        this.marketingContentGateway = marketingContentGateway;
        this.decorationEventAppender = decorationEventAppender;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DecorationModels.PageView createPage(String storefrontId, DecorationModels.CreatePageCommand command) {
        validateCreatePageCommand(command);
        FoundationStorefrontGateway.StorefrontView storefront = foundationStorefrontGateway.getStorefront(storefrontId);
        validatePageOwnership(storefront.storefrontType(), command.pageType());
        if (pageRepository.findByStorefrontIdAndPageCodeAndTerminalType(storefrontId, command.pageCode(), command.terminalType()).isPresent()) {
            throw new IllegalArgumentException("页面已存在: " + command.pageCode() + "@" + command.terminalType());
        }
        OffsetDateTime now = OffsetDateTime.now();
        DecorationPageEntity page = new DecorationPageEntity(
                UUID.randomUUID().toString(),
                storefrontId,
                command.pageCode(),
                command.pageType(),
                command.terminalType(),
                command.pageName(),
                "DRAFTING",
                null,
                now,
                now
        );
        pageRepository.save(page);
        return toPageView(page);
    }

    @Transactional(readOnly = true)
    public List<DecorationModels.PageView> listPages(String storefrontId,
                                                     String terminalType,
                                                     String pageType) {
        Stream<DecorationPageEntity> stream = pageRepository.findByStorefrontIdOrderByCreatedAtDesc(storefrontId).stream();
        if (hasText(terminalType)) {
            stream = stream.filter(page -> terminalType.equals(page.getTerminalType()));
        }
        if (hasText(pageType)) {
            stream = stream.filter(page -> pageType.equals(page.getPageType()));
        }
        return stream.map(this::toPageView).toList();
    }

    @Transactional
    public DecorationModels.DraftView createDraft(String storefrontId,
                                                  String pageId,
                                                  DecorationModels.CreateDraftCommand command) {
        validateDraftCommand(command.layoutConfig(), command.componentTree(), command.localeContentMap(), command.operatorId());
        DecorationPageEntity page = loadPage(storefrontId, pageId);
        ensureOperationAllowed(storefrontId, page.getTerminalType(), "EDIT");
        draftRepository.findFirstByPageIdAndDraftStatusInOrderByDraftVersionDesc(pageId, WORKING_DRAFT_STATUSES)
                .ifPresent(existing -> {
                    throw new IllegalStateException("当前页面已有工作态草稿: " + existing.getDraftId());
                });
        int nextVersion = draftRepository.findFirstByPageIdOrderByDraftVersionDesc(pageId)
                .map(existing -> existing.getDraftVersion() + 1)
                .orElse(1);
        OffsetDateTime now = OffsetDateTime.now();
        DecorationDraftEntity draft = new DecorationDraftEntity(
                UUID.randomUUID().toString(),
                pageId,
                nextVersion,
                "EDITING",
                writeJson(command.layoutConfig()),
                writeJson(command.componentTree()),
                writeJson(command.localeContentMap()),
                writeJson(command.themeConfig()),
                writeJson(command.navigationConfig()),
                "PENDING",
                null,
                now,
                command.operatorId()
        );
        draftRepository.save(draft);
        decorationEventAppender.append("DecorationDraft", draft.getDraftId(), "DecorationDraftSaved", nextVersion, Map.of(
                "draftId", draft.getDraftId(),
                "pageId", pageId,
                "draftVersion", nextVersion
        ));
        return toDraftView(draft);
    }

    @Transactional
    public DecorationModels.DraftView updateDraft(String storefrontId,
                                                  String pageId,
                                                  String draftId,
                                                  DecorationModels.UpdateDraftCommand command) {
        validateDraftCommand(command.layoutConfig(), command.componentTree(), command.localeContentMap(), command.operatorId());
        DecorationPageEntity page = loadPage(storefrontId, pageId);
        ensureOperationAllowed(storefrontId, page.getTerminalType(), "EDIT");
        DecorationDraftEntity draft = loadDraft(pageId, draftId);
        draft.update(
                writeJson(command.layoutConfig()),
                writeJson(command.componentTree()),
                writeJson(command.localeContentMap()),
                writeJson(command.themeConfig()),
                writeJson(command.navigationConfig()),
                command.operatorId(),
                OffsetDateTime.now()
        );
        draftRepository.save(draft);
        decorationEventAppender.append("DecorationDraft", draft.getDraftId(), "DecorationDraftSaved", draft.getDraftVersion(), Map.of(
                "draftId", draft.getDraftId(),
                "pageId", pageId,
                "draftVersion", draft.getDraftVersion()
        ));
        return toDraftView(draft);
    }

    @Transactional
    public DecorationModels.ReviewView submitReview(String storefrontId, String pageId, String draftId, String operatorId) {
        DecorationPageEntity page = loadPage(storefrontId, pageId);
        ensureOperationAllowed(storefrontId, page.getTerminalType(), "SUBMIT");
        DecorationDraftEntity draft = loadDraft(pageId, draftId);
        draft.markSubmitted(operatorId, OffsetDateTime.now());
        draftRepository.save(draft);
        DecorationReviewRecordEntity review = new DecorationReviewRecordEntity(
                UUID.randomUUID().toString(),
                draftId,
                "SUBMITTED",
                null,
                null,
                null
        );
        reviewRecordRepository.save(review);
        decorationEventAppender.append("DecorationReview", review.getReviewId(), "DecorationDraftSubmittedForReview", draft.getDraftVersion(), Map.of(
                "reviewId", review.getReviewId(),
                "draftId", draftId,
                "pageId", pageId
        ));
        return toReviewView(review);
    }

    @Transactional
    public DecorationModels.ReviewView decideReview(String storefrontId,
                                                    String reviewId,
                                                    DecorationModels.ReviewDecisionCommand command) {
        if (command == null || !hasText(command.decision()) || !hasText(command.reviewerId())) {
            throw new IllegalArgumentException("审核决定不完整");
        }
        DecorationReviewRecordEntity review = reviewRecordRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("审核单不存在: " + reviewId));
        DecorationDraftEntity draft = draftRepository.findById(review.getDraftId())
                .orElseThrow(() -> new IllegalArgumentException("草稿不存在: " + review.getDraftId()));
        DecorationPageEntity page = loadPage(storefrontId, draft.getPageId());
        ensurePageBelongsToStorefront(page, storefrontId);
        OffsetDateTime reviewedAt = OffsetDateTime.now();
        if ("APPROVED".equals(command.decision())) {
            draft.markApproved(command.reviewerId(), reviewedAt);
            review.decideApproved(command.reviewerId(), command.comment(), reviewedAt);
        } else if ("REJECTED".equals(command.decision())) {
            draft.markRejected(command.reviewerId(), reviewedAt);
            review.decideRejected(command.reviewerId(), command.comment(), reviewedAt);
        } else {
            throw new IllegalArgumentException("不支持的审核决定: " + command.decision());
        }
        draftRepository.save(draft);
        reviewRecordRepository.save(review);
        return toReviewView(review);
    }

    @Transactional
    public DecorationModels.PublishView publish(String storefrontId,
                                                String pageId,
                                                DecorationModels.PublishPageCommand command) {
        validatePublishCommand(command);
        Optional<DecorationPublishRecordEntity> existingRecord = publishRecordRepository.findByOperationRequestId(command.operationRequestId());
        if (existingRecord.isPresent() && hasText(existingRecord.get().getResultSnapshotId())) {
            DecorationSnapshotEntity snapshot = snapshotRepository.findById(existingRecord.get().getResultSnapshotId())
                    .orElseThrow(() -> new IllegalStateException("发布记录缺少快照: " + existingRecord.get().getResultSnapshotId()));
            return new DecorationModels.PublishView(
                    pageId,
                    existingRecord.get().getDraftId(),
                    snapshot.getSnapshotId(),
                    snapshot.getSnapshotVersion(),
                    snapshot.getTerminalType(),
                    "PUBLISHED",
                    snapshot.getPublishedAt()
            );
        }
        DecorationPageEntity page = loadPage(storefrontId, pageId);
        if (!page.getTerminalType().equals(command.targetTerminalType())) {
            throw new IllegalArgumentException("发布终端与页面终端不一致");
        }
        ensureOperationAllowed(storefrontId, page.getTerminalType(), "PUBLISH");
        DecorationDraftEntity draft = loadDraft(pageId, command.draftId());
        if (!"APPROVED".equals(draft.getDraftStatus())) {
            throw new DecorationRejectedException("DRAFT_NOT_APPROVED", "草稿未通过审核，禁止发布");
        }
        DecorationReviewRecordEntity review = reviewRecordRepository.findById(command.reviewId())
                .orElseThrow(() -> new IllegalArgumentException("审核单不存在: " + command.reviewId()));
        if (!draft.getDraftId().equals(review.getDraftId()) || !"APPROVED".equals(review.getReviewStatus())) {
            throw new DecorationRejectedException("REVIEW_NOT_APPROVED", "审核单未通过，禁止发布");
        }
        FoundationStorefrontGateway.StorefrontView storefront = foundationStorefrontGateway.getStorefront(storefrontId);
        validateReferencedProducts(draft.getComponentTree(), storefront.defaultLocale());
        List<Map<String, Object>> resolvedMarketingObjects = validateAndResolveReferencedMarketingObjects(
                draft.getComponentTree(),
                storefront.defaultLocale(),
                storefront.storefrontType(),
                storefrontId
        );
        int nextSnapshotVersion = snapshotRepository.findFirstByPageIdAndTerminalTypeOrderBySnapshotVersionDesc(pageId, page.getTerminalType())
                .map(existing -> existing.getSnapshotVersion() + 1)
                .orElse(1);
        Map<String, Object> publishedPayload = Map.of(
                "layoutConfig", readJsonMap(draft.getLayoutConfig()),
                "componentTree", readJsonMap(draft.getComponentTree()),
                "localeContentMap", readJsonMap(draft.getLocaleContentMap()),
                "themeConfig", readJsonMap(draft.getThemeConfig()),
                "navigationConfig", readJsonMap(draft.getNavigationConfig()),
                "resolvedMarketingObjects", resolvedMarketingObjects
        );
        String serializedPayload = writeJson(publishedPayload);
        OffsetDateTime now = OffsetDateTime.now();
        DecorationSnapshotEntity snapshot = new DecorationSnapshotEntity(
                UUID.randomUUID().toString(),
                pageId,
                page.getTerminalType(),
                nextSnapshotVersion,
                draft.getDraftId(),
                serializedPayload,
                sha256(serializedPayload),
                "ACTIVE",
                command.operatorId(),
                now
        );
        snapshotRepository.save(snapshot);
        draft.markPublished(command.operatorId(), now);
        draftRepository.save(draft);
        page.publish(snapshot.getSnapshotId(), now);
        pageRepository.save(page);
        DecorationPublishRecordEntity publishRecord = new DecorationPublishRecordEntity(
                UUID.randomUUID().toString(),
                pageId,
                draft.getDraftId(),
                review.getReviewId(),
                "PUBLISH",
                "PUBLISHED",
                command.operationRequestId(),
                command.targetTerminalType(),
                null,
                snapshot.getSnapshotId(),
                null,
                command.operatorId(),
                command.publishComment(),
                now,
                now
        );
        publishRecordRepository.save(publishRecord);
        decorationEventAppender.append("DecorationPage", pageId, "DecorationPublished", nextSnapshotVersion, Map.of(
                "pageId", pageId,
                "snapshotId", snapshot.getSnapshotId(),
                "snapshotVersion", nextSnapshotVersion,
                "terminalType", page.getTerminalType()
        ));
        return new DecorationModels.PublishView(
                pageId,
                draft.getDraftId(),
                snapshot.getSnapshotId(),
                nextSnapshotVersion,
                page.getTerminalType(),
                page.getPageStatus(),
                snapshot.getPublishedAt()
        );
    }

    @Transactional(readOnly = true)
    public DecorationModels.PublishedSnapshotView getPublishedSnapshot(String storefrontId,
                                                                       String pageId,
                                                                       String terminalType,
                                                                       String snapshotId) {
        DecorationPageEntity page = loadPage(storefrontId, pageId);
        if (!page.getTerminalType().equals(terminalType)) {
            throw new IllegalArgumentException("终端与页面不匹配: " + terminalType);
        }
        DecorationSnapshotEntity snapshot = hasText(snapshotId)
                ? snapshotRepository.findById(snapshotId)
                    .orElseThrow(() -> new IllegalArgumentException("快照不存在: " + snapshotId))
                : loadCurrentSnapshot(page);
        return new DecorationModels.PublishedSnapshotView(
                pageId,
                storefrontId,
                page.getPageType(),
                terminalType,
                snapshot.getSnapshotId(),
                snapshot.getSnapshotVersion(),
                readJsonMap(snapshot.getPublishedPayload()),
                snapshot.getPublishedAt()
        );
    }

    @Transactional(readOnly = true)
    public DecorationModels.SnapshotVerificationView verifyPublishedSnapshot(String storefrontId,
                                                                             String pageId,
                                                                             String terminalType,
                                                                             String snapshotId,
                                                                             String expectedChecksum) {
        DecorationPageEntity page = loadPage(storefrontId, pageId);
        if (!page.getTerminalType().equals(terminalType)) {
            throw new IllegalArgumentException("终端与页面不匹配: " + terminalType);
        }
        DecorationSnapshotEntity snapshot = hasText(snapshotId)
                ? snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("快照不存在: " + snapshotId))
                : loadCurrentSnapshot(page);
        boolean checksumMatched = !hasText(expectedChecksum) || snapshot.getPayloadChecksum().equals(expectedChecksum);
        return new DecorationModels.SnapshotVerificationView(
                pageId,
                storefrontId,
                terminalType,
                snapshot.getSnapshotId(),
                snapshot.getSnapshotVersion(),
                snapshot.getPayloadChecksum(),
                expectedChecksum,
                checksumMatched,
                checksumMatched ? "CHECKSUM_MATCH" : "CHECKSUM_MISMATCH"
        );
    }

    private DecorationPageEntity loadPage(String storefrontId, String pageId) {
        DecorationPageEntity page = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalArgumentException("页面不存在: " + pageId));
        ensurePageBelongsToStorefront(page, storefrontId);
        return page;
    }

    private DecorationDraftEntity loadDraft(String pageId, String draftId) {
        DecorationDraftEntity draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("草稿不存在: " + draftId));
        if (!pageId.equals(draft.getPageId())) {
            throw new IllegalArgumentException("草稿不归属当前页面: " + draftId);
        }
        return draft;
    }

    private DecorationSnapshotEntity loadCurrentSnapshot(DecorationPageEntity page) {
        if (!hasText(page.getCurrentSnapshotId())) {
            throw new IllegalArgumentException("当前页面不存在可读发布快照: " + page.getPageId());
        }
        return snapshotRepository.findById(page.getCurrentSnapshotId())
                .orElseThrow(() -> new IllegalArgumentException("当前快照不存在: " + page.getCurrentSnapshotId()));
    }

    private void validateReferencedProducts(String componentTree, String defaultLocale) {
        List<String> productViewIds = extractProductViewIds(componentTree);
        if (productViewIds.isEmpty()) {
            return;
        }
        List<CatalogDecorationProjectionGateway.DecorationProduct> products =
                catalogDecorationProjectionGateway.getProducts(productViewIds, hasText(defaultLocale) ? defaultLocale : "zh-CN");
        if (products.size() != productViewIds.size()) {
            throw new DecorationRejectedException("PRODUCT_PROJECTION_MISSING", "商品投影不可用，禁止发布");
        }
        Set<String> returnedIds = new LinkedHashSet<>();
        for (CatalogDecorationProjectionGateway.DecorationProduct product : products) {
            if (!"COMPLETE".equals(product.contentCompleteness())) {
                throw new DecorationRejectedException("PRODUCT_PROJECTION_INCOMPLETE", "商品投影不可用，禁止发布");
            }
            returnedIds.add(product.productViewId());
        }
        if (!returnedIds.containsAll(productViewIds)) {
            throw new DecorationRejectedException("PRODUCT_PROJECTION_MISSING", "商品投影不可用，禁止发布");
        }
    }

    private List<String> extractProductViewIds(String componentTree) {
        if (!hasText(componentTree)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(componentTree);
            Set<String> productViewIds = new LinkedHashSet<>();
            collectProductViewIds(root, productViewIds);
            return new ArrayList<>(productViewIds);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("组件树不是合法 JSON");
        }
    }

    private List<Map<String, Object>> validateAndResolveReferencedMarketingObjects(String componentTree,
                                                                                   String defaultLocale,
                                                                                   String storefrontType,
                                                                                   String storefrontId) {
        List<MarketingContentGateway.MarketingObjectRef> marketingObjectRefs = extractMarketingObjectRefs(componentTree);
        if (marketingObjectRefs.isEmpty()) {
            return List.of();
        }
        try {
            List<MarketingContentGateway.ResolvedMarketingObject> resolvedObjects = marketingContentGateway.resolveObjects(
                    marketingObjectRefs,
                    hasText(defaultLocale) ? defaultLocale : "zh-CN",
                    "PUBLISH_VALIDATE",
                    storefrontType,
                    storefrontId
            );
            if (resolvedObjects.size() != marketingObjectRefs.size()) {
                throw new DecorationRejectedException("MARKETING_OBJECT_MISSING", "营销对象不可用，禁止发布");
            }
            List<Map<String, Object>> payloads = new ArrayList<>();
            for (MarketingContentGateway.ResolvedMarketingObject object : resolvedObjects) {
                if (!object.publishReady()) {
                    throw new DecorationRejectedException("MARKETING_OBJECT_NOT_READY", "营销对象不可用，禁止发布");
                }
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("objectId", object.objectId());
                payload.put("objectType", object.objectType());
                payload.put("resolvedLocale", object.resolvedLocale());
                payload.put("fallbackApplied", object.fallbackApplied());
                payload.put("publicationStatus", object.publicationStatus());
                payload.put("publishReady", object.publishReady());
                payload.put("payload", object.payload());
                payloads.add(Map.copyOf(payload));
            }
            return List.copyOf(payloads);
        } catch (DecorationRemoteException exception) {
            throw new DecorationRejectedException("MARKETING_VALIDATION_UNAVAILABLE", "营销对象校验不可用，禁止发布");
        }
    }

    private List<MarketingContentGateway.MarketingObjectRef> extractMarketingObjectRefs(String componentTree) {
        if (!hasText(componentTree)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(componentTree);
            Set<String> slotIds = new LinkedHashSet<>();
            collectMarketingObjectRefs(root, slotIds);
            return slotIds.stream()
                    .map(slotId -> new MarketingContentGateway.MarketingObjectRef("MarketingContentSlot", slotId))
                    .toList();
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("组件树不是合法 JSON");
        }
    }

    private void collectMarketingObjectRefs(JsonNode node, Set<String> slotIds) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode componentTypeNode = node.get("componentType");
            JsonNode contentSlotIdNode = node.get("contentSlotId");
            if (componentTypeNode != null
                    && componentTypeNode.isTextual()
                    && "MARKETING_CONTENT_SLOT".equals(componentTypeNode.asText())
                    && contentSlotIdNode != null
                    && contentSlotIdNode.isTextual()) {
                slotIds.add(contentSlotIdNode.asText());
            }
            node.fields().forEachRemaining(entry -> collectMarketingObjectRefs(entry.getValue(), slotIds));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectMarketingObjectRefs(child, slotIds));
        }
    }

    private void collectProductViewIds(JsonNode node, Set<String> productViewIds) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode productViewId = node.get("productViewId");
            if (productViewId != null && productViewId.isTextual()) {
                productViewIds.add(productViewId.asText());
            }
            node.fields().forEachRemaining(entry -> collectProductViewIds(entry.getValue(), productViewIds));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectProductViewIds(child, productViewIds));
        }
    }

    private void validatePageOwnership(String storefrontType, String pageType) {
        if (List.of("PLATFORM_HOME", "PLATFORM_TOPIC").contains(pageType) && !"PLATFORM".equals(storefrontType)) {
            throw new IllegalArgumentException("页面类型不允许归属该店铺: " + pageType);
        }
        if ("STOREFRONT_HOME".equals(pageType) && "PLATFORM".equals(storefrontType)) {
            throw new IllegalArgumentException("页面类型不允许归属该店铺: " + pageType);
        }
    }

    private void ensureOperationAllowed(String storefrontId, String terminalType, String operation) {
        FoundationStorefrontGateway.StorefrontOperability operability =
                foundationStorefrontGateway.getOperability(storefrontId, terminalType, operation);
        if (!operability.allowed()) {
            throw new DecorationRejectedException("STOREFRONT_NOT_OPERABLE", "店铺状态不允许执行装修操作");
        }
    }

    private void ensurePageBelongsToStorefront(DecorationPageEntity page, String storefrontId) {
        if (!storefrontId.equals(page.getStorefrontId())) {
            throw new IllegalArgumentException("页面不归属当前店铺: " + page.getPageId());
        }
    }

    private void validateCreatePageCommand(DecorationModels.CreatePageCommand command) {
        if (command == null
                || !hasText(command.pageCode())
                || !hasText(command.pageType())
                || !hasText(command.terminalType())
                || !hasText(command.pageName())
                || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("创建页面命令不完整");
        }
    }

    private void validateDraftCommand(Map<String, Object> layoutConfig,
                                      Map<String, Object> componentTree,
                                      Map<String, Object> localeContentMap,
                                      String operatorId) {
        if (layoutConfig == null || componentTree == null || localeContentMap == null || !hasText(operatorId)) {
            throw new IllegalArgumentException("草稿内容不完整");
        }
    }

    private void validatePublishCommand(DecorationModels.PublishPageCommand command) {
        if (command == null
                || !hasText(command.draftId())
                || !hasText(command.reviewId())
                || !hasText(command.targetTerminalType())
                || !hasText(command.operationRequestId())
                || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("发布命令不完整");
        }
    }

    private DecorationModels.PageView toPageView(DecorationPageEntity page) {
        return new DecorationModels.PageView(
                page.getPageId(),
                page.getStorefrontId(),
                page.getPageCode(),
                page.getPageType(),
                page.getTerminalType(),
                page.getPageName(),
                page.getPageStatus()
        );
    }

    private DecorationModels.DraftView toDraftView(DecorationDraftEntity draft) {
        return new DecorationModels.DraftView(
                draft.getDraftId(),
                draft.getPageId(),
                draft.getDraftVersion(),
                draft.getDraftStatus(),
                draft.getValidationStatus()
        );
    }

    private DecorationModels.ReviewView toReviewView(DecorationReviewRecordEntity review) {
        return new DecorationModels.ReviewView(
                review.getReviewId(),
                review.getDraftId(),
                review.getReviewStatus(),
                review.getReviewerId(),
                review.getReviewComment()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化装修内容", exception);
        }
    }

    private Map<String, Object> readJsonMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析装修 JSON", exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法计算快照校验和", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
