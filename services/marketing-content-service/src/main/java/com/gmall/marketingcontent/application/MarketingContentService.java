package com.gmall.marketingcontent.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingCampaignBannerEntity;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingCampaignBannerRepository;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingContentSlotEntity;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingContentSlotRepository;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingProjectionCacheEntity;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingProjectionCacheRepository;
import com.gmall.marketingcontent.infrastructure.persistence.TopicContentBlockEntity;
import com.gmall.marketingcontent.infrastructure.persistence.TopicContentBlockRepository;
import com.gmall.shared.localization.LocalizedFieldPayload;
import com.gmall.shared.localization.LocalizationFieldPolicy;
import com.gmall.shared.localization.LocalizationResolution;
import com.gmall.shared.localization.LocalizationResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingContentService {

    private static final String OBJECT_TYPE_CONTENT_SLOT = "MarketingContentSlot";
    private static final String OBJECT_TYPE_CAMPAIGN_BANNER = "MarketingCampaignBanner";
    private static final String OBJECT_TYPE_TOPIC_BLOCK = "TopicContentBlock";

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_LIVE = "LIVE";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private static final String SCHEDULE_TYPE_ALWAYS_ON = "ALWAYS_ON";
    private static final String SCHEDULE_TYPE_TIMEBOXED = "TIMEBOXED";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private static final String PROJECTION_HOME = "HOME";
    private static final String PROJECTION_TOPIC = "TOPIC";

    private final MarketingCampaignBannerRepository campaignBannerRepository;
    private final MarketingContentSlotRepository contentSlotRepository;
    private final TopicContentBlockRepository topicContentBlockRepository;
    private final MarketingProjectionCacheRepository projectionCacheRepository;
    private final LocalizationResolver localizationResolver;
    private final ObjectMapper objectMapper;
    private final int projectionCacheTtlSeconds;

    @Autowired
    public MarketingContentService(MarketingCampaignBannerRepository campaignBannerRepository,
                                   MarketingContentSlotRepository contentSlotRepository,
                                   TopicContentBlockRepository topicContentBlockRepository,
                                   MarketingProjectionCacheRepository projectionCacheRepository,
                                   @Value("${gmall.marketing.projection-cache-ttl-seconds:120}") int projectionCacheTtlSeconds) {
        this.campaignBannerRepository = campaignBannerRepository;
        this.contentSlotRepository = contentSlotRepository;
        this.topicContentBlockRepository = topicContentBlockRepository;
        this.projectionCacheRepository = projectionCacheRepository;
        this.localizationResolver = new LocalizationResolver();
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.projectionCacheTtlSeconds = projectionCacheTtlSeconds;
    }

    @Transactional
    public MarketingContentModels.CampaignBannerView createCampaignBanner(MarketingContentModels.CreateCampaignBannerCommand command) {
        validateCreateCampaignBannerCommand(command);
        campaignBannerRepository.findByOwnerTypeAndOwnerIdAndCampaignCode(command.ownerType(), command.ownerId(), command.campaignCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("活动编码已存在: " + command.campaignCode());
                });
        OffsetDateTime now = OffsetDateTime.now();
        MarketingCampaignBannerEntity entity = new MarketingCampaignBannerEntity(
                UUID.randomUUID().toString(),
                command.campaignCode(),
                command.campaignType(),
                command.ownerType(),
                command.ownerId(),
                writeJson(normalizeLocalizedText(command.title())),
                writeJson(normalizeLocalizedText(command.subTitle())),
                writeJson(normalizeJsonMap(command.bannerImage())),
                writeJson(normalizeLandingTarget(command.landingTarget())),
                writeJson(normalizePublicationWindow(command.publicationWindow())),
                STATUS_DRAFT,
                1,
                command.operatorId(),
                now,
                command.operatorId(),
                now
        );
        campaignBannerRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toCampaignBannerView(entity);
    }

    @Transactional
    public MarketingContentModels.CampaignBannerView updateCampaignBanner(String campaignId,
                                                                          MarketingContentModels.UpdateCampaignBannerCommand command) {
        validateUpdateCampaignBannerCommand(command);
        MarketingCampaignBannerEntity entity = loadCampaignBanner(campaignId);
        entity.update(
                writeJson(normalizeLocalizedText(command.title())),
                writeJson(normalizeLocalizedText(command.subTitle())),
                writeJson(normalizeJsonMap(command.bannerImage())),
                writeJson(normalizeLandingTarget(command.landingTarget())),
                writeJson(normalizePublicationWindow(command.publicationWindow())),
                command.operatorId(),
                OffsetDateTime.now()
        );
        campaignBannerRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toCampaignBannerView(entity);
    }

    @Transactional(readOnly = true)
    public MarketingContentModels.CampaignBannerView getCampaignBanner(String campaignId) {
        return toCampaignBannerView(loadCampaignBanner(campaignId));
    }

    @Transactional(readOnly = true)
    public List<MarketingContentModels.CampaignBannerView> listCampaignBanners(String ownerType, String ownerId, String campaignStatus) {
        Stream<MarketingCampaignBannerEntity> stream = campaignBannerRepository.findAllByOrderByUpdatedAtDesc().stream();
        if (hasText(ownerType)) {
            stream = stream.filter(item -> ownerType.equals(item.getOwnerType()));
        }
        if (hasText(ownerId)) {
            stream = stream.filter(item -> ownerId.equals(item.getOwnerId()));
        }
        if (hasText(campaignStatus)) {
            stream = stream.filter(item -> campaignStatus.equals(item.getCampaignStatus()));
        }
        return stream.map(this::toCampaignBannerView).toList();
    }

    @Transactional
    public MarketingContentModels.CampaignBannerView scheduleCampaignBanner(String campaignId,
                                                                            MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        MarketingCampaignBannerEntity entity = loadCampaignBanner(campaignId);
        PublicationWindowState window = toPublicationWindowState(command.publicationWindow());
        if (!SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) || !OffsetDateTime.now().isBefore(window.effectiveFrom())) {
            throw new IllegalArgumentException("排期上线必须配置未来生效时间");
        }
        ensureBannerPublishable(entity, window, LocalizationResolver.DEFAULT_LOCALE);
        entity.schedule(writeJson(toPublicationWindowMap(window)), command.operatorId(), OffsetDateTime.now());
        campaignBannerRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toCampaignBannerView(entity);
    }

    @Transactional
    public MarketingContentModels.CampaignBannerView goLiveCampaignBanner(String campaignId,
                                                                          MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        MarketingCampaignBannerEntity entity = loadCampaignBanner(campaignId);
        PublicationWindowState window = command.publicationWindow() == null
                ? toPublicationWindowState(readJsonMap(entity.getPublicationWindow()))
                : toPublicationWindowState(command.publicationWindow());
        ensureBannerPublishable(entity, window, LocalizationResolver.DEFAULT_LOCALE);
        OffsetDateTime now = OffsetDateTime.now();
        String targetStatus = deriveStatusByWindow(window, now);
        if (STATUS_EXPIRED.equals(targetStatus)) {
            throw new MarketingContentRejectedException("WINDOW_EXPIRED", "投放窗口已过期，禁止上线");
        }
        if (STATUS_SCHEDULED.equals(targetStatus)) {
            entity.schedule(writeJson(toPublicationWindowMap(window)), command.operatorId(), now);
        } else {
            entity.goLive(command.operatorId(), now);
            entity.update(entity.getTitleI18n(), entity.getSubTitleI18n(), entity.getBannerImage(), entity.getLandingTarget(), writeJson(toPublicationWindowMap(window)), command.operatorId(), now);
        }
        campaignBannerRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toCampaignBannerView(entity);
    }

    @Transactional
    public MarketingContentModels.CampaignBannerView offlineCampaignBanner(String campaignId,
                                                                           MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        MarketingCampaignBannerEntity entity = loadCampaignBanner(campaignId);
        entity.offline(command.operatorId(), OffsetDateTime.now());
        campaignBannerRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toCampaignBannerView(entity);
    }

    @Transactional
    public MarketingContentModels.ContentSlotView createContentSlot(MarketingContentModels.CreateContentSlotCommand command) {
        validateCreateContentSlotCommand(command);
        contentSlotRepository.findByOwnerTypeAndOwnerIdAndSlotCode(command.ownerType(), command.ownerId(), command.slotCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("内容位编码已存在: " + command.slotCode());
                });
        OffsetDateTime now = OffsetDateTime.now();
        MarketingContentSlotEntity slot = new MarketingContentSlotEntity(
                UUID.randomUUID().toString(),
                command.slotCode(),
                command.slotType(),
                command.ownerType(),
                command.ownerId(),
                writeJson(normalizeLocalizedText(command.title())),
                writeJson(normalizeLocalizedText(command.body())),
                writeJson(normalizeMediaList(command.mediaList())),
                writeJson(normalizeLandingTarget(command.landingTarget())),
                writeJson(normalizePublicationWindow(command.publicationWindow())),
                STATUS_DRAFT,
                1,
                command.operatorId(),
                now,
                command.operatorId(),
                now
        );
        contentSlotRepository.save(slot);
        evictProjectionCache(slot.getOwnerType(), slot.getOwnerId());
        return toContentSlotView(slot);
    }

    @Transactional
    public MarketingContentModels.ContentSlotView updateContentSlot(String contentSlotId,
                                                                    MarketingContentModels.UpdateContentSlotCommand command) {
        validateUpdateContentSlotCommand(command);
        MarketingContentSlotEntity slot = loadContentSlot(contentSlotId);
        slot.update(
                writeJson(normalizeLocalizedText(command.title())),
                writeJson(normalizeLocalizedText(command.body())),
                writeJson(normalizeMediaList(command.mediaList())),
                writeJson(normalizeLandingTarget(command.landingTarget())),
                writeJson(normalizePublicationWindow(command.publicationWindow())),
                command.operatorId(),
                OffsetDateTime.now()
        );
        contentSlotRepository.save(slot);
        evictProjectionCache(slot.getOwnerType(), slot.getOwnerId());
        return toContentSlotView(slot);
    }

    @Transactional(readOnly = true)
    public MarketingContentModels.ContentSlotView getContentSlot(String contentSlotId) {
        return toContentSlotView(loadContentSlot(contentSlotId));
    }

    @Transactional(readOnly = true)
    public List<MarketingContentModels.ContentSlotView> listContentSlots(String ownerType, String ownerId, String slotStatus) {
        Stream<MarketingContentSlotEntity> stream = contentSlotRepository.findAllByOrderByUpdatedAtDesc().stream();
        if (hasText(ownerType)) {
            stream = stream.filter(item -> ownerType.equals(item.getOwnerType()));
        }
        if (hasText(ownerId)) {
            stream = stream.filter(item -> ownerId.equals(item.getOwnerId()));
        }
        if (hasText(slotStatus)) {
            stream = stream.filter(item -> slotStatus.equals(item.getSlotStatus()));
        }
        return stream.map(this::toContentSlotView).toList();
    }

    @Transactional
    public MarketingContentModels.ContentSlotView scheduleContentSlot(String contentSlotId,
                                                                      MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        MarketingContentSlotEntity slot = loadContentSlot(contentSlotId);
        PublicationWindowState window = toPublicationWindowState(command.publicationWindow());
        if (!SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) || !OffsetDateTime.now().isBefore(window.effectiveFrom())) {
            throw new IllegalArgumentException("排期上线必须配置未来生效时间");
        }
        ensureSlotPublishable(slot, window, LocalizationResolver.DEFAULT_LOCALE);
        slot.schedule(writeJson(toPublicationWindowMap(window)), command.operatorId(), OffsetDateTime.now());
        contentSlotRepository.save(slot);
        evictProjectionCache(slot.getOwnerType(), slot.getOwnerId());
        return toContentSlotView(slot);
    }

    @Transactional
    public MarketingContentModels.ContentSlotView goLiveContentSlot(String contentSlotId,
                                                                    MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        MarketingContentSlotEntity slot = loadContentSlot(contentSlotId);
        PublicationWindowState window = command.publicationWindow() == null
                ? toPublicationWindowState(readJsonMap(slot.getPublicationWindow()))
                : toPublicationWindowState(command.publicationWindow());
        ensureSlotPublishable(slot, window, LocalizationResolver.DEFAULT_LOCALE);
        OffsetDateTime now = OffsetDateTime.now();
        String targetStatus = deriveStatusByWindow(window, now);
        if (STATUS_EXPIRED.equals(targetStatus)) {
            throw new MarketingContentRejectedException("WINDOW_EXPIRED", "投放窗口已过期，禁止上线");
        }
        if (STATUS_SCHEDULED.equals(targetStatus)) {
            slot.schedule(writeJson(toPublicationWindowMap(window)), command.operatorId(), now);
        } else {
            slot.goLive(command.operatorId(), now);
            slot.update(slot.getTitleI18n(), slot.getBodyI18n(), slot.getMediaList(), slot.getLandingTarget(), writeJson(toPublicationWindowMap(window)), command.operatorId(), now);
        }
        contentSlotRepository.save(slot);
        evictProjectionCache(slot.getOwnerType(), slot.getOwnerId());
        return toContentSlotView(slot);
    }

    @Transactional
    public MarketingContentModels.ContentSlotView offlineContentSlot(String contentSlotId,
                                                                     MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        MarketingContentSlotEntity slot = loadContentSlot(contentSlotId);
        slot.offline(command.operatorId(), OffsetDateTime.now());
        contentSlotRepository.save(slot);
        evictProjectionCache(slot.getOwnerType(), slot.getOwnerId());
        return toContentSlotView(slot);
    }

    @Transactional
    public MarketingContentModels.TopicContentBlockView createTopicContentBlock(MarketingContentModels.CreateTopicContentBlockCommand command) {
        validateCreateTopicContentBlockCommand(command);
        topicContentBlockRepository.findByOwnerTypeAndOwnerIdAndTopicCode(command.ownerType(), command.ownerId(), command.topicCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("专题编码已存在: " + command.topicCode());
                });
        OffsetDateTime now = OffsetDateTime.now();
        TopicContentBlockEntity entity = new TopicContentBlockEntity(
                UUID.randomUUID().toString(),
                command.topicCode(),
                command.topicType(),
                command.ownerType(),
                command.ownerId(),
                writeJson(normalizeLocalizedText(command.topicTitle())),
                writeJson(normalizeLocalizedText(command.topicSummary())),
                writeJson(normalizeJsonMap(command.heroImage())),
                writeJson(normalizeMapList(command.contentBlocks())),
                writeJson(normalizeLandingTarget(command.landingTarget())),
                writeJson(normalizePublicationWindow(command.publicationWindow())),
                STATUS_DRAFT,
                1,
                command.operatorId(),
                now,
                command.operatorId(),
                now
        );
        topicContentBlockRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toTopicContentBlockView(entity);
    }

    @Transactional
    public MarketingContentModels.TopicContentBlockView updateTopicContentBlock(String topicBlockId,
                                                                                MarketingContentModels.UpdateTopicContentBlockCommand command) {
        validateUpdateTopicContentBlockCommand(command);
        TopicContentBlockEntity entity = loadTopicContentBlock(topicBlockId);
        entity.update(
                writeJson(normalizeLocalizedText(command.topicTitle())),
                writeJson(normalizeLocalizedText(command.topicSummary())),
                writeJson(normalizeJsonMap(command.heroImage())),
                writeJson(normalizeMapList(command.contentBlocks())),
                writeJson(normalizeLandingTarget(command.landingTarget())),
                writeJson(normalizePublicationWindow(command.publicationWindow())),
                command.operatorId(),
                OffsetDateTime.now()
        );
        topicContentBlockRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toTopicContentBlockView(entity);
    }

    @Transactional(readOnly = true)
    public MarketingContentModels.TopicContentBlockView getTopicContentBlock(String topicBlockId) {
        return toTopicContentBlockView(loadTopicContentBlock(topicBlockId));
    }

    @Transactional(readOnly = true)
    public List<MarketingContentModels.TopicContentBlockView> listTopicContentBlocks(String ownerType, String ownerId, String topicStatus) {
        Stream<TopicContentBlockEntity> stream = topicContentBlockRepository.findAllByOrderByUpdatedAtDesc().stream();
        if (hasText(ownerType)) {
            stream = stream.filter(item -> ownerType.equals(item.getOwnerType()));
        }
        if (hasText(ownerId)) {
            stream = stream.filter(item -> ownerId.equals(item.getOwnerId()));
        }
        if (hasText(topicStatus)) {
            stream = stream.filter(item -> topicStatus.equals(item.getTopicStatus()));
        }
        return stream.map(this::toTopicContentBlockView).toList();
    }

    @Transactional
    public MarketingContentModels.TopicContentBlockView scheduleTopicContentBlock(String topicBlockId,
                                                                                  MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        TopicContentBlockEntity entity = loadTopicContentBlock(topicBlockId);
        PublicationWindowState window = toPublicationWindowState(command.publicationWindow());
        if (!SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) || !OffsetDateTime.now().isBefore(window.effectiveFrom())) {
            throw new IllegalArgumentException("排期上线必须配置未来生效时间");
        }
        ensureTopicPublishable(entity, window, LocalizationResolver.DEFAULT_LOCALE);
        entity.schedule(writeJson(toPublicationWindowMap(window)), command.operatorId(), OffsetDateTime.now());
        topicContentBlockRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toTopicContentBlockView(entity);
    }

    @Transactional
    public MarketingContentModels.TopicContentBlockView goLiveTopicContentBlock(String topicBlockId,
                                                                                MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        TopicContentBlockEntity entity = loadTopicContentBlock(topicBlockId);
        PublicationWindowState window = command.publicationWindow() == null
                ? toPublicationWindowState(readJsonMap(entity.getPublicationWindow()))
                : toPublicationWindowState(command.publicationWindow());
        ensureTopicPublishable(entity, window, LocalizationResolver.DEFAULT_LOCALE);
        OffsetDateTime now = OffsetDateTime.now();
        String targetStatus = deriveStatusByWindow(window, now);
        if (STATUS_EXPIRED.equals(targetStatus)) {
            throw new MarketingContentRejectedException("WINDOW_EXPIRED", "投放窗口已过期，禁止上线");
        }
        if (STATUS_SCHEDULED.equals(targetStatus)) {
            entity.schedule(writeJson(toPublicationWindowMap(window)), command.operatorId(), now);
        } else {
            entity.goLive(command.operatorId(), now);
            entity.update(entity.getTopicTitleI18n(), entity.getTopicSummaryI18n(), entity.getHeroImage(), entity.getContentBlocks(), entity.getLandingTarget(), writeJson(toPublicationWindowMap(window)), command.operatorId(), now);
        }
        topicContentBlockRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toTopicContentBlockView(entity);
    }

    @Transactional
    public MarketingContentModels.TopicContentBlockView offlineTopicContentBlock(String topicBlockId,
                                                                                 MarketingContentModels.ChangeObjectStatusCommand command) {
        validateStatusChangeCommand(command);
        TopicContentBlockEntity entity = loadTopicContentBlock(topicBlockId);
        entity.offline(command.operatorId(), OffsetDateTime.now());
        topicContentBlockRepository.save(entity);
        evictProjectionCache(entity.getOwnerType(), entity.getOwnerId());
        return toTopicContentBlockView(entity);
    }

    @Transactional(readOnly = true)
    public List<MarketingContentModels.ResolvedMarketingObjectView> resolveMarketingObjects(
            MarketingContentModels.ResolveMarketingObjectsCommand command) {
        validateResolveCommand(command);
        String locale = hasText(command.locale()) ? command.locale() : LocalizationResolver.DEFAULT_LOCALE;
        boolean publishValidate = "PUBLISH_VALIDATE".equals(command.resolveMode());
        OffsetDateTime now = OffsetDateTime.now();
        List<MarketingContentModels.ResolvedMarketingObjectView> resolvedObjects = new ArrayList<>();
        for (MarketingContentModels.MarketingObjectRef objectRef : command.objectRefs()) {
            resolvedObjects.add(resolveSingleObject(objectRef, command.ownerContext(), locale, publishValidate, now));
        }
        return resolvedObjects;
    }

    @Transactional(readOnly = true)
    public MarketingContentModels.ProjectionResultView getMarketingProjections(MarketingContentModels.ProjectionQueryView query) {
        validateProjectionQuery(query);
        String locale = hasText(query.locale()) ? query.locale() : LocalizationResolver.DEFAULT_LOCALE;
        String terminalType = hasText(query.terminalType()) ? query.terminalType() : "MOBILE";
        String pageContext = hasText(query.pageContext()) ? query.pageContext() : "DEFAULT";
        Optional<MarketingProjectionCacheEntity> optionalCache = projectionCacheRepository
                .findByProjectionTypeAndOwnerTypeAndOwnerIdAndTerminalTypeAndPageContextAndLocale(
                        query.projectionType(),
                        query.ownerType(),
                        query.ownerId(),
                        terminalType,
                        pageContext,
                        locale
                );
        OffsetDateTime now = OffsetDateTime.now();
        if (optionalCache.isPresent() && !isCacheExpired(optionalCache.get(), now)) {
            return new MarketingContentModels.ProjectionResultView(
                    readMapList(optionalCache.get().getPayload()),
                    optionalCache.get().getResolvedLocale(),
                    optionalCache.get().isFallbackApplied(),
                    optionalCache.get().getCacheTtlSeconds()
            );
        }

        ProjectionBuildResult built = buildProjection(query.projectionType(), query.ownerType(), query.ownerId(), locale, pageContext, now);
        String serializedPayload = writeJson(built.items());
        MarketingProjectionCacheEntity cache = optionalCache.orElseGet(() -> new MarketingProjectionCacheEntity(
                UUID.randomUUID().toString(),
                query.projectionType(),
                query.ownerType(),
                query.ownerId(),
                terminalType,
                pageContext,
                locale,
                built.resolvedLocale(),
                built.fallbackApplied(),
                projectionCacheTtlSeconds,
                serializedPayload,
                now
        ));
        cache.refresh(built.resolvedLocale(), built.fallbackApplied(), projectionCacheTtlSeconds, serializedPayload, now);
        projectionCacheRepository.save(cache);

        return new MarketingContentModels.ProjectionResultView(
                built.items(),
                built.resolvedLocale(),
                built.fallbackApplied(),
                projectionCacheTtlSeconds
        );
    }

    @Scheduled(
            fixedDelayString = "${gmall.marketing.schedule.scan-interval-ms:30000}",
            initialDelayString = "${gmall.marketing.schedule.initial-delay-ms:0}"
    )
    @Transactional
    public void applyPublicationSchedule() {
        OffsetDateTime now = OffsetDateTime.now();
        Set<OwnerKey> dirtyOwners = new LinkedHashSet<>();

        for (MarketingCampaignBannerEntity entity : campaignBannerRepository.findByCampaignStatusOrderByUpdatedAtDesc(STATUS_SCHEDULED)) {
            PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
            if (!SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType())) {
                continue;
            }
            if (!now.isBefore(window.effectiveTo())) {
                entity.expire("scheduler", now);
                campaignBannerRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            } else if (!now.isBefore(window.effectiveFrom())) {
                entity.goLive("scheduler", now);
                campaignBannerRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            }
        }

        for (MarketingCampaignBannerEntity entity : campaignBannerRepository.findByCampaignStatusOrderByUpdatedAtDesc(STATUS_LIVE)) {
            PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
            if (SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) && !now.isBefore(window.effectiveTo())) {
                entity.expire("scheduler", now);
                campaignBannerRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            }
        }

        for (MarketingContentSlotEntity entity : contentSlotRepository.findBySlotStatusOrderByUpdatedAtDesc(STATUS_SCHEDULED)) {
            PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
            if (!SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType())) {
                continue;
            }
            if (!now.isBefore(window.effectiveTo())) {
                entity.expire("scheduler", now);
                contentSlotRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            } else if (!now.isBefore(window.effectiveFrom())) {
                entity.goLive("scheduler", now);
                contentSlotRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            }
        }

        for (MarketingContentSlotEntity entity : contentSlotRepository.findBySlotStatusOrderByUpdatedAtDesc(STATUS_LIVE)) {
            PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
            if (SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) && !now.isBefore(window.effectiveTo())) {
                entity.expire("scheduler", now);
                contentSlotRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            }
        }

        for (TopicContentBlockEntity entity : topicContentBlockRepository.findByTopicStatusOrderByUpdatedAtDesc(STATUS_SCHEDULED)) {
            PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
            if (!SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType())) {
                continue;
            }
            if (!now.isBefore(window.effectiveTo())) {
                entity.expire("scheduler", now);
                topicContentBlockRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            } else if (!now.isBefore(window.effectiveFrom())) {
                entity.goLive("scheduler", now);
                topicContentBlockRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            }
        }

        for (TopicContentBlockEntity entity : topicContentBlockRepository.findByTopicStatusOrderByUpdatedAtDesc(STATUS_LIVE)) {
            PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
            if (SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) && !now.isBefore(window.effectiveTo())) {
                entity.expire("scheduler", now);
                topicContentBlockRepository.save(entity);
                dirtyOwners.add(new OwnerKey(entity.getOwnerType(), entity.getOwnerId()));
            }
        }

        for (OwnerKey owner : dirtyOwners) {
            evictProjectionCache(owner.ownerType(), owner.ownerId());
        }
    }

    private MarketingContentModels.ResolvedMarketingObjectView resolveSingleObject(MarketingContentModels.MarketingObjectRef objectRef,
                                                                                    MarketingContentModels.OwnerContext ownerContext,
                                                                                    String locale,
                                                                                    boolean publishValidate,
                                                                                    OffsetDateTime now) {
        return switch (objectRef.objectType()) {
            case OBJECT_TYPE_CAMPAIGN_BANNER -> resolveCampaignBanner(objectRef.objectId(), ownerContext, locale, publishValidate, now);
            case OBJECT_TYPE_CONTENT_SLOT -> resolveContentSlot(objectRef.objectId(), ownerContext, locale, publishValidate, now);
            case OBJECT_TYPE_TOPIC_BLOCK -> resolveTopicContentBlock(objectRef.objectId(), ownerContext, locale, publishValidate, now);
            default -> throw new IllegalArgumentException("不支持的营销对象类型: " + objectRef.objectType());
        };
    }

    private MarketingContentModels.ResolvedMarketingObjectView resolveCampaignBanner(String campaignId,
                                                                                      MarketingContentModels.OwnerContext ownerContext,
                                                                                      String locale,
                                                                                      boolean publishValidate,
                                                                                      OffsetDateTime now) {
        Optional<MarketingCampaignBannerEntity> optional = campaignBannerRepository.findById(campaignId);
        if (optional.isEmpty()) {
            return handleMissingObject(campaignId, OBJECT_TYPE_CAMPAIGN_BANNER, locale, publishValidate);
        }
        MarketingCampaignBannerEntity entity = optional.get();
        boolean ownerMatched = matchesOwnerContext(entity.getOwnerType(), entity.getOwnerId(), ownerContext);
        if (publishValidate && !ownerMatched) {
            throw new MarketingContentRejectedException("OWNER_CONTEXT_MISMATCH", "营销对象归属不匹配: " + campaignId);
        }
        LocalizationResolution titleResolution = localizationResolver.resolve(
                readLocalizedText(entity.getTitleI18n()),
                locale,
                LocalizationFieldPolicy.required("title")
        );
        LocalizationResolution subTitleResolution = localizationResolver.resolve(
                readLocalizedText(entity.getSubTitleI18n()),
                locale,
                LocalizationFieldPolicy.optional("subTitle")
        );
        PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
        String publicationStatus = derivePublicationStatus(entity.getCampaignStatus(), window, now);
        boolean publishReady = STATUS_LIVE.equals(publicationStatus)
                && ownerMatched
                && titleResolution.publishReady()
                && isLandingTargetValid(readJsonMap(entity.getLandingTarget()));
        if (publishValidate && !publishReady) {
            throw new MarketingContentRejectedException("OBJECT_NOT_PUBLISH_READY", "营销对象不可用于发布: " + campaignId);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignId", entity.getCampaignId());
        payload.put("campaignCode", entity.getCampaignCode());
        payload.put("campaignType", entity.getCampaignType());
        payload.put("title", titleResolution.toFieldPayload());
        payload.put("subTitle", subTitleResolution.toFieldPayload());
        payload.put("bannerImage", readJsonMap(entity.getBannerImage()));
        payload.put("landingTarget", readJsonMap(entity.getLandingTarget()));
        return new MarketingContentModels.ResolvedMarketingObjectView(
                campaignId,
                OBJECT_TYPE_CAMPAIGN_BANNER,
                hasText(titleResolution.resolvedLocale()) ? titleResolution.resolvedLocale() : locale,
                titleResolution.fallbackApplied() || subTitleResolution.fallbackApplied(),
                publicationStatus,
                publishReady,
                payload
        );
    }

    private MarketingContentModels.ResolvedMarketingObjectView resolveContentSlot(String contentSlotId,
                                                                                   MarketingContentModels.OwnerContext ownerContext,
                                                                                   String locale,
                                                                                   boolean publishValidate,
                                                                                   OffsetDateTime now) {
        Optional<MarketingContentSlotEntity> optional = contentSlotRepository.findById(contentSlotId);
        if (optional.isEmpty()) {
            return handleMissingObject(contentSlotId, OBJECT_TYPE_CONTENT_SLOT, locale, publishValidate);
        }
        MarketingContentSlotEntity slot = optional.get();
        boolean ownerMatched = matchesOwnerContext(slot.getOwnerType(), slot.getOwnerId(), ownerContext);
        if (publishValidate && !ownerMatched) {
            throw new MarketingContentRejectedException("OWNER_CONTEXT_MISMATCH", "营销对象归属不匹配: " + contentSlotId);
        }
        LocalizationResolution titleResolution = localizationResolver.resolve(
                readLocalizedText(slot.getTitleI18n()),
                locale,
                LocalizationFieldPolicy.required("title")
        );
        LocalizationResolution bodyResolution = localizationResolver.resolve(
                readLocalizedText(slot.getBodyI18n()),
                locale,
                LocalizationFieldPolicy.optional("body")
        );
        PublicationWindowState window = toPublicationWindowState(readJsonMap(slot.getPublicationWindow()));
        String publicationStatus = derivePublicationStatus(slot.getSlotStatus(), window, now);
        boolean publishReady = STATUS_LIVE.equals(publicationStatus)
                && ownerMatched
                && titleResolution.publishReady()
                && isLandingTargetValid(readJsonMap(slot.getLandingTarget()));
        if (publishValidate && !publishReady) {
            throw new MarketingContentRejectedException("OBJECT_NOT_PUBLISH_READY", "营销对象不可用于发布: " + contentSlotId);
        }
        return new MarketingContentModels.ResolvedMarketingObjectView(
                contentSlotId,
                OBJECT_TYPE_CONTENT_SLOT,
                hasText(titleResolution.resolvedLocale()) ? titleResolution.resolvedLocale() : locale,
                titleResolution.fallbackApplied() || bodyResolution.fallbackApplied(),
                publicationStatus,
                publishReady,
                buildContentSlotPayload(slot, titleResolution.toFieldPayload(), bodyResolution.toFieldPayload())
        );
    }

    private MarketingContentModels.ResolvedMarketingObjectView resolveTopicContentBlock(String topicBlockId,
                                                                                         MarketingContentModels.OwnerContext ownerContext,
                                                                                         String locale,
                                                                                         boolean publishValidate,
                                                                                         OffsetDateTime now) {
        Optional<TopicContentBlockEntity> optional = topicContentBlockRepository.findById(topicBlockId);
        if (optional.isEmpty()) {
            return handleMissingObject(topicBlockId, OBJECT_TYPE_TOPIC_BLOCK, locale, publishValidate);
        }
        TopicContentBlockEntity entity = optional.get();
        boolean ownerMatched = matchesOwnerContext(entity.getOwnerType(), entity.getOwnerId(), ownerContext);
        if (publishValidate && !ownerMatched) {
            throw new MarketingContentRejectedException("OWNER_CONTEXT_MISMATCH", "营销对象归属不匹配: " + topicBlockId);
        }
        LocalizationResolution titleResolution = localizationResolver.resolve(
                readLocalizedText(entity.getTopicTitleI18n()),
                locale,
                LocalizationFieldPolicy.required("topicTitle")
        );
        LocalizationResolution summaryResolution = localizationResolver.resolve(
                readLocalizedText(entity.getTopicSummaryI18n()),
                locale,
                LocalizationFieldPolicy.optional("topicSummary")
        );
        PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
        String publicationStatus = derivePublicationStatus(entity.getTopicStatus(), window, now);
        boolean publishReady = STATUS_LIVE.equals(publicationStatus)
                && ownerMatched
                && titleResolution.publishReady()
                && isLandingTargetValid(readJsonMap(entity.getLandingTarget()));
        if (publishValidate && !publishReady) {
            throw new MarketingContentRejectedException("OBJECT_NOT_PUBLISH_READY", "营销对象不可用于发布: " + topicBlockId);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("topicBlockId", entity.getTopicBlockId());
        payload.put("topicCode", entity.getTopicCode());
        payload.put("topicType", entity.getTopicType());
        payload.put("topicTitle", titleResolution.toFieldPayload());
        payload.put("topicSummary", summaryResolution.toFieldPayload());
        payload.put("heroImage", readJsonMap(entity.getHeroImage()));
        payload.put("contentBlocks", readMapList(entity.getContentBlocks()));
        payload.put("landingTarget", readJsonMap(entity.getLandingTarget()));
        return new MarketingContentModels.ResolvedMarketingObjectView(
                topicBlockId,
                OBJECT_TYPE_TOPIC_BLOCK,
                hasText(titleResolution.resolvedLocale()) ? titleResolution.resolvedLocale() : locale,
                titleResolution.fallbackApplied() || summaryResolution.fallbackApplied(),
                publicationStatus,
                publishReady,
                payload
        );
    }

    private MarketingContentModels.ResolvedMarketingObjectView handleMissingObject(String objectId,
                                                                                   String objectType,
                                                                                   String locale,
                                                                                   boolean publishValidate) {
        if (publishValidate) {
            throw new MarketingContentRejectedException("OBJECT_NOT_FOUND", "营销对象不存在: " + objectId);
        }
        return new MarketingContentModels.ResolvedMarketingObjectView(
                objectId,
                objectType,
                locale,
                false,
                "NOT_FOUND",
                false,
                Map.of()
        );
    }

    private ProjectionBuildResult buildProjection(String projectionType,
                                                  String ownerType,
                                                  String ownerId,
                                                  String locale,
                                                  String pageContext,
                                                  OffsetDateTime now) {
        if (PROJECTION_HOME.equals(projectionType)) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (MarketingCampaignBannerEntity entity : campaignBannerRepository
                    .findByOwnerTypeAndOwnerIdAndCampaignStatusOrderByUpdatedAtDesc(ownerType, ownerId, STATUS_LIVE)) {
                PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
                if (!STATUS_LIVE.equals(derivePublicationStatus(entity.getCampaignStatus(), window, now))) {
                    continue;
                }
                LocalizationResolution titleResolution = localizationResolver.resolve(
                        readLocalizedText(entity.getTitleI18n()),
                        locale,
                        LocalizationFieldPolicy.required("title")
                );
                LocalizationResolution subTitleResolution = localizationResolver.resolve(
                        readLocalizedText(entity.getSubTitleI18n()),
                        locale,
                        LocalizationFieldPolicy.optional("subTitle")
                );
                if (!titleResolution.publishReady() || !isLandingTargetValid(readJsonMap(entity.getLandingTarget()))) {
                    continue;
                }
                items.add(toProjectionItem(
                        entity.getCampaignId(),
                        OBJECT_TYPE_CAMPAIGN_BANNER,
                        hasText(titleResolution.resolvedLocale()) ? titleResolution.resolvedLocale() : locale,
                        titleResolution.fallbackApplied() || subTitleResolution.fallbackApplied(),
                        Map.of(
                                "campaignId", entity.getCampaignId(),
                                "campaignCode", entity.getCampaignCode(),
                                "campaignType", entity.getCampaignType(),
                                "title", titleResolution.toFieldPayload(),
                                "subTitle", subTitleResolution.toFieldPayload(),
                                "bannerImage", readJsonMap(entity.getBannerImage()),
                                "landingTarget", readJsonMap(entity.getLandingTarget())
                        )
                ));
            }
            for (MarketingContentSlotEntity entity : contentSlotRepository
                    .findByOwnerTypeAndOwnerIdAndSlotStatusOrderByUpdatedAtDesc(ownerType, ownerId, STATUS_LIVE)) {
                PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
                if (!STATUS_LIVE.equals(derivePublicationStatus(entity.getSlotStatus(), window, now))) {
                    continue;
                }
                LocalizationResolution titleResolution = localizationResolver.resolve(
                        readLocalizedText(entity.getTitleI18n()),
                        locale,
                        LocalizationFieldPolicy.required("title")
                );
                LocalizationResolution bodyResolution = localizationResolver.resolve(
                        readLocalizedText(entity.getBodyI18n()),
                        locale,
                        LocalizationFieldPolicy.optional("body")
                );
                if (!titleResolution.publishReady() || !isLandingTargetValid(readJsonMap(entity.getLandingTarget()))) {
                    continue;
                }
                items.add(toProjectionItem(
                        entity.getContentSlotId(),
                        OBJECT_TYPE_CONTENT_SLOT,
                        hasText(titleResolution.resolvedLocale()) ? titleResolution.resolvedLocale() : locale,
                        titleResolution.fallbackApplied() || bodyResolution.fallbackApplied(),
                        buildContentSlotPayload(entity, titleResolution.toFieldPayload(), bodyResolution.toFieldPayload())
                ));
            }
            return summarizeProjection(items, locale);
        }

        if (PROJECTION_TOPIC.equals(projectionType)) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (TopicContentBlockEntity entity : topicContentBlockRepository
                    .findByOwnerTypeAndOwnerIdAndTopicStatusOrderByUpdatedAtDesc(ownerType, ownerId, STATUS_LIVE)) {
                PublicationWindowState window = toPublicationWindowState(readJsonMap(entity.getPublicationWindow()));
                if (!STATUS_LIVE.equals(derivePublicationStatus(entity.getTopicStatus(), window, now))) {
                    continue;
                }
                LocalizationResolution titleResolution = localizationResolver.resolve(
                        readLocalizedText(entity.getTopicTitleI18n()),
                        locale,
                        LocalizationFieldPolicy.required("topicTitle")
                );
                LocalizationResolution summaryResolution = localizationResolver.resolve(
                        readLocalizedText(entity.getTopicSummaryI18n()),
                        locale,
                        LocalizationFieldPolicy.optional("topicSummary")
                );
                if (!titleResolution.publishReady() || !isLandingTargetValid(readJsonMap(entity.getLandingTarget()))) {
                    continue;
                }
                items.add(toProjectionItem(
                        entity.getTopicBlockId(),
                        OBJECT_TYPE_TOPIC_BLOCK,
                        hasText(titleResolution.resolvedLocale()) ? titleResolution.resolvedLocale() : locale,
                        titleResolution.fallbackApplied() || summaryResolution.fallbackApplied(),
                        Map.of(
                                "topicBlockId", entity.getTopicBlockId(),
                                "topicCode", entity.getTopicCode(),
                                "topicType", entity.getTopicType(),
                                "topicTitle", titleResolution.toFieldPayload(),
                                "topicSummary", summaryResolution.toFieldPayload(),
                                "heroImage", readJsonMap(entity.getHeroImage()),
                                "contentBlocks", readMapList(entity.getContentBlocks()),
                                "landingTarget", readJsonMap(entity.getLandingTarget()),
                                "pageContext", pageContext
                        )
                ));
            }
            return summarizeProjection(items, locale);
        }

        throw new IllegalArgumentException("不支持的投影类型: " + projectionType);
    }

    private ProjectionBuildResult summarizeProjection(List<Map<String, Object>> items, String locale) {
        boolean fallbackApplied = false;
        String resolvedLocale = locale;
        if (!items.isEmpty()) {
            Object resolvedLocaleValue = items.get(0).get("resolvedLocale");
            if (resolvedLocaleValue instanceof String value && hasText(value)) {
                resolvedLocale = value;
            }
        }
        for (Map<String, Object> item : items) {
            Object value = item.get("fallbackApplied");
            if (value instanceof Boolean bool && bool) {
                fallbackApplied = true;
                break;
            }
        }
        return new ProjectionBuildResult(items, resolvedLocale, fallbackApplied);
    }

    private Map<String, Object> toProjectionItem(String objectId,
                                                 String objectType,
                                                 String resolvedLocale,
                                                 boolean fallbackApplied,
                                                 Map<String, Object> payload) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("objectId", objectId);
        item.put("objectType", objectType);
        item.put("resolvedLocale", resolvedLocale);
        item.put("fallbackApplied", fallbackApplied);
        item.put("publicationStatus", STATUS_LIVE);
        item.put("payload", payload);
        return item;
    }

    private Map<String, Object> buildContentSlotPayload(MarketingContentSlotEntity slot,
                                                        LocalizedFieldPayload title,
                                                        LocalizedFieldPayload body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contentSlotId", slot.getContentSlotId());
        payload.put("slotCode", slot.getSlotCode());
        payload.put("slotType", slot.getSlotType());
        payload.put("title", title);
        payload.put("body", body);
        payload.put("mediaList", readStringList(slot.getMediaList()));
        payload.put("landingTarget", readJsonMap(slot.getLandingTarget()));
        return payload;
    }

    private MarketingCampaignBannerEntity loadCampaignBanner(String campaignId) {
        return campaignBannerRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("活动横幅不存在: " + campaignId));
    }

    private MarketingContentSlotEntity loadContentSlot(String contentSlotId) {
        return contentSlotRepository.findById(contentSlotId)
                .orElseThrow(() -> new IllegalArgumentException("内容位不存在: " + contentSlotId));
    }

    private TopicContentBlockEntity loadTopicContentBlock(String topicBlockId) {
        return topicContentBlockRepository.findById(topicBlockId)
                .orElseThrow(() -> new IllegalArgumentException("专题内容块不存在: " + topicBlockId));
    }

    private MarketingContentModels.CampaignBannerView toCampaignBannerView(MarketingCampaignBannerEntity entity) {
        return new MarketingContentModels.CampaignBannerView(
                entity.getCampaignId(),
                entity.getCampaignCode(),
                entity.getCampaignType(),
                entity.getOwnerType(),
                entity.getOwnerId(),
                readLocalizedText(entity.getTitleI18n()),
                readLocalizedText(entity.getSubTitleI18n()),
                readJsonMap(entity.getBannerImage()),
                readJsonMap(entity.getLandingTarget()),
                readJsonMap(entity.getPublicationWindow()),
                entity.getCampaignStatus(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private MarketingContentModels.ContentSlotView toContentSlotView(MarketingContentSlotEntity slot) {
        return new MarketingContentModels.ContentSlotView(
                slot.getContentSlotId(),
                slot.getSlotCode(),
                slot.getSlotType(),
                slot.getOwnerType(),
                slot.getOwnerId(),
                readLocalizedText(slot.getTitleI18n()),
                readLocalizedText(slot.getBodyI18n()),
                readStringList(slot.getMediaList()),
                readJsonMap(slot.getLandingTarget()),
                readJsonMap(slot.getPublicationWindow()),
                slot.getSlotStatus(),
                slot.getVersion(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }

    private MarketingContentModels.TopicContentBlockView toTopicContentBlockView(TopicContentBlockEntity entity) {
        return new MarketingContentModels.TopicContentBlockView(
                entity.getTopicBlockId(),
                entity.getTopicCode(),
                entity.getTopicType(),
                entity.getOwnerType(),
                entity.getOwnerId(),
                readLocalizedText(entity.getTopicTitleI18n()),
                readLocalizedText(entity.getTopicSummaryI18n()),
                readJsonMap(entity.getHeroImage()),
                readMapList(entity.getContentBlocks()),
                readJsonMap(entity.getLandingTarget()),
                readJsonMap(entity.getPublicationWindow()),
                entity.getTopicStatus(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void validateCreateCampaignBannerCommand(MarketingContentModels.CreateCampaignBannerCommand command) {
        if (command == null
                || !hasText(command.campaignCode())
                || !hasText(command.campaignType())
                || !hasText(command.ownerType())
                || !hasText(command.ownerId())
                || command.title() == null
                || command.subTitle() == null
                || command.bannerImage() == null
                || command.landingTarget() == null
                || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("活动横幅创建命令不完整");
        }
        toPublicationWindowState(command.publicationWindow());
    }

    private void validateUpdateCampaignBannerCommand(MarketingContentModels.UpdateCampaignBannerCommand command) {
        if (command == null
                || command.title() == null
                || command.subTitle() == null
                || command.bannerImage() == null
                || command.landingTarget() == null
                || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("活动横幅更新命令不完整");
        }
        toPublicationWindowState(command.publicationWindow());
    }

    private void validateCreateContentSlotCommand(MarketingContentModels.CreateContentSlotCommand command) {
        if (command == null
                || !hasText(command.slotCode())
                || !hasText(command.slotType())
                || !hasText(command.ownerType())
                || !hasText(command.ownerId())
                || command.title() == null
                || command.body() == null
                || command.mediaList() == null
                || command.landingTarget() == null
                || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("内容位创建命令不完整");
        }
        toPublicationWindowState(command.publicationWindow());
    }

    private void validateUpdateContentSlotCommand(MarketingContentModels.UpdateContentSlotCommand command) {
        if (command == null
                || command.title() == null
                || command.body() == null
                || command.mediaList() == null
                || command.landingTarget() == null
                || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("内容位更新命令不完整");
        }
        toPublicationWindowState(command.publicationWindow());
    }

    private void validateCreateTopicContentBlockCommand(MarketingContentModels.CreateTopicContentBlockCommand command) {
        if (command == null
                || !hasText(command.topicCode())
                || !hasText(command.topicType())
                || !hasText(command.ownerType())
                || !hasText(command.ownerId())
                || command.topicTitle() == null
                || command.topicSummary() == null
                || command.heroImage() == null
                || command.contentBlocks() == null
                || command.landingTarget() == null
                || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("专题内容块创建命令不完整");
        }
        toPublicationWindowState(command.publicationWindow());
    }

    private void validateUpdateTopicContentBlockCommand(MarketingContentModels.UpdateTopicContentBlockCommand command) {
        if (command == null
                || command.topicTitle() == null
                || command.topicSummary() == null
                || command.heroImage() == null
                || command.contentBlocks() == null
                || command.landingTarget() == null
                || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("专题内容块更新命令不完整");
        }
        toPublicationWindowState(command.publicationWindow());
    }

    private void validateStatusChangeCommand(MarketingContentModels.ChangeObjectStatusCommand command) {
        if (command == null || !hasText(command.operatorId())) {
            throw new IllegalArgumentException("状态变更命令不完整");
        }
    }

    private void validateResolveCommand(MarketingContentModels.ResolveMarketingObjectsCommand command) {
        if (command == null
                || !hasText(command.resolveMode())
                || command.objectRefs() == null
                || command.objectRefs().isEmpty()) {
            throw new IllegalArgumentException("营销对象解析命令不完整");
        }
        if (!List.of("PREVIEW", "PUBLISH_VALIDATE").contains(command.resolveMode())) {
            throw new IllegalArgumentException("不支持的解析模式: " + command.resolveMode());
        }
        if ("PUBLISH_VALIDATE".equals(command.resolveMode())) {
            if (command.ownerContext() == null
                    || !hasText(command.ownerContext().ownerType())
                    || !hasText(command.ownerContext().ownerId())) {
                throw new IllegalArgumentException("发布校验必须提供 ownerContext");
            }
        }
    }

    private void validateProjectionQuery(MarketingContentModels.ProjectionQueryView query) {
        if (query == null
                || !hasText(query.projectionType())
                || !hasText(query.ownerType())
                || !hasText(query.ownerId())) {
            throw new IllegalArgumentException("营销投影查询参数不完整");
        }
        if (!List.of(PROJECTION_HOME, PROJECTION_TOPIC).contains(query.projectionType())) {
            throw new IllegalArgumentException("不支持的投影类型: " + query.projectionType());
        }
    }

    private void ensureBannerPublishable(MarketingCampaignBannerEntity entity,
                                         PublicationWindowState window,
                                         String locale) {
        LocalizationResolution titleResolution = localizationResolver.resolve(
                readLocalizedText(entity.getTitleI18n()),
                locale,
                LocalizationFieldPolicy.required("title")
        );
        if (!titleResolution.publishReady()) {
            throw new MarketingContentRejectedException("TITLE_TRANSLATION_INCOMPLETE", "关键标题缺少双语文案，禁止上线");
        }
        if (!isLandingTargetValid(readJsonMap(entity.getLandingTarget()))) {
            throw new MarketingContentRejectedException("LANDING_TARGET_INVALID", "落地页配置不完整，禁止上线");
        }
        if (SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) && !window.effectiveFrom().isBefore(window.effectiveTo())) {
            throw new IllegalArgumentException("投放窗口时间非法");
        }
    }

    private void ensureSlotPublishable(MarketingContentSlotEntity entity,
                                       PublicationWindowState window,
                                       String locale) {
        LocalizationResolution titleResolution = localizationResolver.resolve(
                readLocalizedText(entity.getTitleI18n()),
                locale,
                LocalizationFieldPolicy.required("title")
        );
        if (!titleResolution.publishReady()) {
            throw new MarketingContentRejectedException("TITLE_TRANSLATION_INCOMPLETE", "关键标题缺少双语文案，禁止上线");
        }
        if (!isLandingTargetValid(readJsonMap(entity.getLandingTarget()))) {
            throw new MarketingContentRejectedException("LANDING_TARGET_INVALID", "落地页配置不完整，禁止上线");
        }
        if (SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) && !window.effectiveFrom().isBefore(window.effectiveTo())) {
            throw new IllegalArgumentException("投放窗口时间非法");
        }
    }

    private void ensureTopicPublishable(TopicContentBlockEntity entity,
                                        PublicationWindowState window,
                                        String locale) {
        LocalizationResolution titleResolution = localizationResolver.resolve(
                readLocalizedText(entity.getTopicTitleI18n()),
                locale,
                LocalizationFieldPolicy.required("topicTitle")
        );
        if (!titleResolution.publishReady()) {
            throw new MarketingContentRejectedException("TITLE_TRANSLATION_INCOMPLETE", "专题标题缺少双语文案，禁止上线");
        }
        if (!isLandingTargetValid(readJsonMap(entity.getLandingTarget()))) {
            throw new MarketingContentRejectedException("LANDING_TARGET_INVALID", "落地页配置不完整，禁止上线");
        }
        if (SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType()) && !window.effectiveFrom().isBefore(window.effectiveTo())) {
            throw new IllegalArgumentException("投放窗口时间非法");
        }
    }

    private PublicationWindowState toPublicationWindowState(Map<String, Object> window) {
        Map<String, Object> normalized = normalizePublicationWindow(window);
        String scheduleType = stringValue(normalized.get("scheduleType"), SCHEDULE_TYPE_ALWAYS_ON);
        if (!List.of(SCHEDULE_TYPE_ALWAYS_ON, SCHEDULE_TYPE_TIMEBOXED).contains(scheduleType)) {
            throw new IllegalArgumentException("不支持的排期类型: " + scheduleType);
        }
        OffsetDateTime effectiveFrom = parseOffsetDateTime(normalized.get("effectiveFrom"));
        OffsetDateTime effectiveTo = parseOffsetDateTime(normalized.get("effectiveTo"));
        if (SCHEDULE_TYPE_TIMEBOXED.equals(scheduleType)) {
            if (effectiveFrom == null || effectiveTo == null) {
                throw new IllegalArgumentException("TIMEBOXED 必须提供 effectiveFrom 与 effectiveTo");
            }
            if (!effectiveFrom.isBefore(effectiveTo)) {
                throw new IllegalArgumentException("effectiveFrom 必须早于 effectiveTo");
            }
        }
        return new PublicationWindowState(
                scheduleType,
                effectiveFrom,
                effectiveTo,
                stringValue(normalized.get("timezone"), DEFAULT_TIMEZONE),
                booleanValue(normalized.get("manualOfflineAllowed"), true)
        );
    }

    private Map<String, Object> toPublicationWindowMap(PublicationWindowState state) {
        Map<String, Object> window = new LinkedHashMap<>();
        window.put("scheduleType", state.scheduleType());
        window.put("timezone", state.timezone());
        window.put("manualOfflineAllowed", state.manualOfflineAllowed());
        if (state.effectiveFrom() != null) {
            window.put("effectiveFrom", state.effectiveFrom());
        }
        if (state.effectiveTo() != null) {
            window.put("effectiveTo", state.effectiveTo());
        }
        return window;
    }

    private String deriveStatusByWindow(PublicationWindowState state, OffsetDateTime now) {
        if (!SCHEDULE_TYPE_TIMEBOXED.equals(state.scheduleType())) {
            return STATUS_LIVE;
        }
        if (now.isBefore(state.effectiveFrom())) {
            return STATUS_SCHEDULED;
        }
        if (!now.isBefore(state.effectiveTo())) {
            return STATUS_EXPIRED;
        }
        return STATUS_LIVE;
    }

    private String derivePublicationStatus(String persistedStatus,
                                           PublicationWindowState window,
                                           OffsetDateTime now) {
        if (!STATUS_LIVE.equals(persistedStatus) && !STATUS_SCHEDULED.equals(persistedStatus)) {
            return persistedStatus;
        }
        if (!SCHEDULE_TYPE_TIMEBOXED.equals(window.scheduleType())) {
            return persistedStatus;
        }
        if (!now.isBefore(window.effectiveTo())) {
            return STATUS_EXPIRED;
        }
        if (STATUS_SCHEDULED.equals(persistedStatus) && !now.isBefore(window.effectiveFrom())) {
            return STATUS_LIVE;
        }
        return persistedStatus;
    }

    private Map<String, String> normalizeLocalizedText(Map<String, String> localizedText) {
        if (localizedText == null || localizedText.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        localizedText.forEach((locale, value) -> {
            if (!hasText(locale)) {
                return;
            }
            normalized.put(locale, value == null ? "" : value.trim());
        });
        return Map.copyOf(normalized);
    }

    private List<String> normalizeMediaList(List<String> mediaList) {
        if (mediaList == null) {
            return List.of();
        }
        return mediaList.stream()
                .filter(this::hasText)
                .map(String::trim)
                .toList();
    }

    private List<Map<String, Object>> normalizeMapList(List<Map<String, Object>> mapList) {
        if (mapList == null) {
            return List.of();
        }
        return mapList.stream()
                .filter(item -> item != null && !item.isEmpty())
                .map(this::normalizeJsonMap)
                .toList();
    }

    private Map<String, Object> normalizeLandingTarget(Map<String, Object> landingTarget) {
        if (landingTarget == null) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(landingTarget));
    }

    private Map<String, Object> normalizeJsonMap(Map<String, Object> jsonMap) {
        if (jsonMap == null) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(jsonMap));
    }

    private Map<String, Object> normalizePublicationWindow(Map<String, Object> window) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (window != null) {
            normalized.putAll(window);
        }
        normalized.putIfAbsent("scheduleType", SCHEDULE_TYPE_ALWAYS_ON);
        normalized.putIfAbsent("timezone", DEFAULT_TIMEZONE);
        normalized.putIfAbsent("manualOfflineAllowed", true);
        return normalized;
    }

    private boolean matchesOwnerContext(String ownerType,
                                        String ownerId,
                                        MarketingContentModels.OwnerContext ownerContext) {
        if (ownerContext == null) {
            return true;
        }
        return ownerType.equals(ownerContext.ownerType()) && ownerId.equals(ownerContext.ownerId());
    }

    private boolean isLandingTargetValid(Map<String, Object> landingTarget) {
        Object targetType = landingTarget.get("targetType");
        Object targetValue = landingTarget.get("targetValue");
        return targetType instanceof String type && hasText(type)
                && targetValue instanceof String value && hasText(value);
    }

    private boolean isCacheExpired(MarketingProjectionCacheEntity cache, OffsetDateTime now) {
        return cache.getLastBuiltAt().plusSeconds(cache.getCacheTtlSeconds()).isBefore(now);
    }

    private void evictProjectionCache(String ownerType, String ownerId) {
        projectionCacheRepository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化营销内容", exception);
        }
    }

    private Map<String, String> readLocalizedText(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析多语言文案", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析字符串列表", exception);
        }
    }

    private List<Map<String, Object>> readMapList(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析对象列表", exception);
        }
    }

    private Map<String, Object> readJsonMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析营销对象 JSON", exception);
        }
    }

    private OffsetDateTime parseOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Number number) {
            BigDecimal numeric = new BigDecimal(number.toString());
            if (numeric.abs().compareTo(BigDecimal.valueOf(1_000_000_000_000L)) >= 0) {
                Instant instant = Instant.ofEpochMilli(number.longValue());
                return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
            }
            long seconds = numeric.longValue();
            int nanos = numeric.subtract(BigDecimal.valueOf(seconds))
                    .movePointRight(9)
                    .abs()
                    .intValue();
            Instant instant = Instant.ofEpochSecond(seconds, nanos);
            return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        if (value instanceof String text && hasText(text)) {
            return OffsetDateTime.parse(text.trim());
        }
        throw new IllegalArgumentException("无法解析时间字段: " + value);
    }

    private String stringValue(Object value, String defaultValue) {
        if (value instanceof String text && hasText(text)) {
            return text.trim();
        }
        return defaultValue;
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && hasText(text)) {
            return Boolean.parseBoolean(text.trim());
        }
        return defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record PublicationWindowState(String scheduleType,
                                          OffsetDateTime effectiveFrom,
                                          OffsetDateTime effectiveTo,
                                          String timezone,
                                          boolean manualOfflineAllowed) {
    }

    private record ProjectionBuildResult(List<Map<String, Object>> items,
                                         String resolvedLocale,
                                         boolean fallbackApplied) {
    }

    private record OwnerKey(String ownerType, String ownerId) {
    }
}
