package com.gmall.marketingcontent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.gmall.marketingcontent.infrastructure.persistence.MarketingCampaignBannerEntity;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingCampaignBannerRepository;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingContentSlotEntity;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingContentSlotRepository;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingProjectionCacheEntity;
import com.gmall.marketingcontent.infrastructure.persistence.MarketingProjectionCacheRepository;
import com.gmall.marketingcontent.infrastructure.persistence.TopicContentBlockEntity;
import com.gmall.marketingcontent.infrastructure.persistence.TopicContentBlockRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketingContentServiceTest {

    private final MarketingCampaignBannerRepository campaignBannerRepository = mock(MarketingCampaignBannerRepository.class);
    private final MarketingContentSlotRepository contentSlotRepository = mock(MarketingContentSlotRepository.class);
    private final TopicContentBlockRepository topicContentBlockRepository = mock(TopicContentBlockRepository.class);
    private final MarketingProjectionCacheRepository projectionCacheRepository = mock(MarketingProjectionCacheRepository.class);

    private final List<MarketingCampaignBannerEntity> savedBanners = new ArrayList<>();
    private final List<MarketingContentSlotEntity> savedSlots = new ArrayList<>();
    private final List<TopicContentBlockEntity> savedTopics = new ArrayList<>();
    private final List<MarketingProjectionCacheEntity> savedCaches = new ArrayList<>();

    private MarketingContentService marketingContentService;

    @BeforeEach
    void setUp() {
        marketingContentService = new MarketingContentService(
                campaignBannerRepository,
                contentSlotRepository,
                topicContentBlockRepository,
                projectionCacheRepository,
                120
        );

        when(campaignBannerRepository.save(any(MarketingCampaignBannerEntity.class))).thenAnswer(invocation -> {
            MarketingCampaignBannerEntity entity = invocation.getArgument(0);
            savedBanners.removeIf(existing -> existing.getCampaignId().equals(entity.getCampaignId()));
            savedBanners.add(entity);
            return entity;
        });
        when(campaignBannerRepository.findById(anyString())).thenAnswer(invocation -> {
            String campaignId = invocation.getArgument(0);
            return savedBanners.stream().filter(item -> item.getCampaignId().equals(campaignId)).findFirst();
        });
        when(campaignBannerRepository.findByOwnerTypeAndOwnerIdAndCampaignCode(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String ownerType = invocation.getArgument(0);
                    String ownerId = invocation.getArgument(1);
                    String campaignCode = invocation.getArgument(2);
                    return savedBanners.stream()
                            .filter(item -> item.getOwnerType().equals(ownerType))
                            .filter(item -> item.getOwnerId().equals(ownerId))
                            .filter(item -> item.getCampaignCode().equals(campaignCode))
                            .findFirst();
                });
        when(campaignBannerRepository.findAllByOrderByUpdatedAtDesc()).thenAnswer(invocation -> savedBanners.stream()
                .sorted(Comparator.comparing(MarketingCampaignBannerEntity::getUpdatedAt).reversed())
                .toList());
        when(campaignBannerRepository.findByOwnerTypeAndOwnerIdAndCampaignStatusOrderByUpdatedAtDesc(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String ownerType = invocation.getArgument(0);
                    String ownerId = invocation.getArgument(1);
                    String status = invocation.getArgument(2);
                    return savedBanners.stream()
                            .filter(item -> item.getOwnerType().equals(ownerType))
                            .filter(item -> item.getOwnerId().equals(ownerId))
                            .filter(item -> item.getCampaignStatus().equals(status))
                            .sorted(Comparator.comparing(MarketingCampaignBannerEntity::getUpdatedAt).reversed())
                            .toList();
                });
        when(campaignBannerRepository.findByCampaignStatusOrderByUpdatedAtDesc(anyString()))
                .thenAnswer(invocation -> {
                    String status = invocation.getArgument(0);
                    return savedBanners.stream()
                            .filter(item -> item.getCampaignStatus().equals(status))
                            .sorted(Comparator.comparing(MarketingCampaignBannerEntity::getUpdatedAt).reversed())
                            .toList();
                });

        when(contentSlotRepository.save(any(MarketingContentSlotEntity.class))).thenAnswer(invocation -> {
            MarketingContentSlotEntity entity = invocation.getArgument(0);
            savedSlots.removeIf(existing -> existing.getContentSlotId().equals(entity.getContentSlotId()));
            savedSlots.add(entity);
            return entity;
        });
        when(contentSlotRepository.findById(anyString())).thenAnswer(invocation -> {
            String slotId = invocation.getArgument(0);
            return savedSlots.stream().filter(item -> item.getContentSlotId().equals(slotId)).findFirst();
        });
        when(contentSlotRepository.findByOwnerTypeAndOwnerIdAndSlotCode(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String ownerType = invocation.getArgument(0);
                    String ownerId = invocation.getArgument(1);
                    String slotCode = invocation.getArgument(2);
                    return savedSlots.stream()
                            .filter(item -> item.getOwnerType().equals(ownerType))
                            .filter(item -> item.getOwnerId().equals(ownerId))
                            .filter(item -> item.getSlotCode().equals(slotCode))
                            .findFirst();
                });
        when(contentSlotRepository.findAllByOrderByUpdatedAtDesc()).thenAnswer(invocation -> savedSlots.stream()
                .sorted(Comparator.comparing(MarketingContentSlotEntity::getUpdatedAt).reversed())
                .toList());
        when(contentSlotRepository.findByOwnerTypeAndOwnerIdAndSlotStatusOrderByUpdatedAtDesc(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String ownerType = invocation.getArgument(0);
                    String ownerId = invocation.getArgument(1);
                    String status = invocation.getArgument(2);
                    return savedSlots.stream()
                            .filter(item -> item.getOwnerType().equals(ownerType))
                            .filter(item -> item.getOwnerId().equals(ownerId))
                            .filter(item -> item.getSlotStatus().equals(status))
                            .sorted(Comparator.comparing(MarketingContentSlotEntity::getUpdatedAt).reversed())
                            .toList();
                });
        when(contentSlotRepository.findBySlotStatusOrderByUpdatedAtDesc(anyString()))
                .thenAnswer(invocation -> {
                    String status = invocation.getArgument(0);
                    return savedSlots.stream()
                            .filter(item -> item.getSlotStatus().equals(status))
                            .sorted(Comparator.comparing(MarketingContentSlotEntity::getUpdatedAt).reversed())
                            .toList();
                });

        when(topicContentBlockRepository.save(any(TopicContentBlockEntity.class))).thenAnswer(invocation -> {
            TopicContentBlockEntity entity = invocation.getArgument(0);
            savedTopics.removeIf(existing -> existing.getTopicBlockId().equals(entity.getTopicBlockId()));
            savedTopics.add(entity);
            return entity;
        });
        when(topicContentBlockRepository.findById(anyString())).thenAnswer(invocation -> {
            String topicId = invocation.getArgument(0);
            return savedTopics.stream().filter(item -> item.getTopicBlockId().equals(topicId)).findFirst();
        });
        when(topicContentBlockRepository.findByOwnerTypeAndOwnerIdAndTopicCode(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String ownerType = invocation.getArgument(0);
                    String ownerId = invocation.getArgument(1);
                    String topicCode = invocation.getArgument(2);
                    return savedTopics.stream()
                            .filter(item -> item.getOwnerType().equals(ownerType))
                            .filter(item -> item.getOwnerId().equals(ownerId))
                            .filter(item -> item.getTopicCode().equals(topicCode))
                            .findFirst();
                });
        when(topicContentBlockRepository.findAllByOrderByUpdatedAtDesc()).thenAnswer(invocation -> savedTopics.stream()
                .sorted(Comparator.comparing(TopicContentBlockEntity::getUpdatedAt).reversed())
                .toList());
        when(topicContentBlockRepository.findByOwnerTypeAndOwnerIdAndTopicStatusOrderByUpdatedAtDesc(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String ownerType = invocation.getArgument(0);
                    String ownerId = invocation.getArgument(1);
                    String status = invocation.getArgument(2);
                    return savedTopics.stream()
                            .filter(item -> item.getOwnerType().equals(ownerType))
                            .filter(item -> item.getOwnerId().equals(ownerId))
                            .filter(item -> item.getTopicStatus().equals(status))
                            .sorted(Comparator.comparing(TopicContentBlockEntity::getUpdatedAt).reversed())
                            .toList();
                });
        when(topicContentBlockRepository.findByTopicStatusOrderByUpdatedAtDesc(anyString()))
                .thenAnswer(invocation -> {
                    String status = invocation.getArgument(0);
                    return savedTopics.stream()
                            .filter(item -> item.getTopicStatus().equals(status))
                            .sorted(Comparator.comparing(TopicContentBlockEntity::getUpdatedAt).reversed())
                            .toList();
                });

        when(projectionCacheRepository.save(any(MarketingProjectionCacheEntity.class))).thenAnswer(invocation -> {
            MarketingProjectionCacheEntity entity = invocation.getArgument(0);
            savedCaches.removeIf(existing -> existing.getCacheId().equals(entity.getCacheId()));
            savedCaches.add(entity);
            return entity;
        });
        when(projectionCacheRepository.findByProjectionTypeAndOwnerTypeAndOwnerIdAndTerminalTypeAndPageContextAndLocale(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String projectionType = invocation.getArgument(0);
                    String ownerType = invocation.getArgument(1);
                    String ownerId = invocation.getArgument(2);
                    String terminalType = invocation.getArgument(3);
                    String pageContext = invocation.getArgument(4);
                    String locale = invocation.getArgument(5);
                    return savedCaches.stream()
                            .filter(item -> item.getProjectionType().equals(projectionType))
                            .filter(item -> item.getOwnerType().equals(ownerType))
                            .filter(item -> item.getOwnerId().equals(ownerId))
                            .filter(item -> item.getTerminalType().equals(terminalType))
                            .filter(item -> item.getPageContext().equals(pageContext))
                            .filter(item -> item.getLocale().equals(locale))
                            .findFirst();
                });
        when(projectionCacheRepository.findById(anyString())).thenAnswer(invocation -> {
            String cacheId = invocation.getArgument(0);
            return savedCaches.stream().filter(item -> item.getCacheId().equals(cacheId)).findFirst();
        });
        when(projectionCacheRepository.findAll()).thenAnswer(invocation -> List.copyOf(savedCaches));
        when(projectionCacheRepository.findAllById(any())).thenAnswer(invocation -> List.copyOf(savedCaches));
        doAnswer(invocation -> {
            String ownerType = invocation.getArgument(0);
            String ownerId = invocation.getArgument(1);
            savedCaches.removeIf(item -> item.getOwnerType().equals(ownerType) && item.getOwnerId().equals(ownerId));
            return null;
        }).when(projectionCacheRepository).deleteByOwnerTypeAndOwnerId(anyString(), anyString());
    }

    @Test
    void resolveSupportsBannerSlotAndTopicInPublishValidate() {
        String ownerId = "merchant-" + UUID.randomUUID();

        MarketingContentModels.CampaignBannerView banner = marketingContentService.createCampaignBanner(
                new MarketingContentModels.CreateCampaignBannerCommand(
                        "CAMP-" + UUID.randomUUID(),
                        "PLATFORM_HOME",
                        "MERCHANT",
                        ownerId,
                        Map.of("zh-CN", "首页活动", "en-US", "Home Campaign"),
                        Map.of("zh-CN", "副标题", "en-US", "Sub Title"),
                        Map.of("url", "https://img.example.com/banner.png"),
                        Map.of("targetType", "URL", "targetValue", "https://gmall.example.com/banner"),
                        Map.of("scheduleType", "ALWAYS_ON"),
                        "operator-1"
                )
        );
        marketingContentService.goLiveCampaignBanner(
                banner.campaignId(),
                new MarketingContentModels.ChangeObjectStatusCommand("operator-2", "上线", Map.of("scheduleType", "ALWAYS_ON"))
        );

        MarketingContentModels.ContentSlotView slot = marketingContentService.createContentSlot(
                new MarketingContentModels.CreateContentSlotCommand(
                        "HOME_SLOT_" + UUID.randomUUID(),
                        "NOTICE",
                        "MERCHANT",
                        ownerId,
                        Map.of("zh-CN", "首页内容位", "en-US", "Home Slot"),
                        Map.of("zh-CN", "内容", "en-US", "Body"),
                        List.of("https://img.example.com/slot.png"),
                        Map.of("targetType", "URL", "targetValue", "https://gmall.example.com/slot"),
                        Map.of("scheduleType", "ALWAYS_ON"),
                        "operator-1"
                )
        );
        marketingContentService.goLiveContentSlot(
                slot.contentSlotId(),
                new MarketingContentModels.ChangeObjectStatusCommand("operator-2", "上线", Map.of("scheduleType", "ALWAYS_ON"))
        );

        MarketingContentModels.TopicContentBlockView topic = marketingContentService.createTopicContentBlock(
                new MarketingContentModels.CreateTopicContentBlockCommand(
                        "TOPIC_" + UUID.randomUUID(),
                        "PLATFORM_TOPIC",
                        "MERCHANT",
                        ownerId,
                        Map.of("zh-CN", "专题标题", "en-US", "Topic Title"),
                        Map.of("zh-CN", "专题摘要", "en-US", "Topic Summary"),
                        Map.of("url", "https://img.example.com/topic.png"),
                        List.of(Map.of("blockType", "TEXT", "value", "hello")),
                        Map.of("targetType", "URL", "targetValue", "https://gmall.example.com/topic"),
                        Map.of("scheduleType", "ALWAYS_ON"),
                        "operator-1"
                )
        );
        marketingContentService.goLiveTopicContentBlock(
                topic.topicBlockId(),
                new MarketingContentModels.ChangeObjectStatusCommand("operator-2", "上线", Map.of("scheduleType", "ALWAYS_ON"))
        );

        List<MarketingContentModels.ResolvedMarketingObjectView> resolved = marketingContentService.resolveMarketingObjects(
                new MarketingContentModels.ResolveMarketingObjectsCommand(
                        "en-US",
                        "PUBLISH_VALIDATE",
                        new MarketingContentModels.OwnerContext("MERCHANT", ownerId),
                        List.of(
                                new MarketingContentModels.MarketingObjectRef("MarketingCampaignBanner", banner.campaignId()),
                                new MarketingContentModels.MarketingObjectRef("MarketingContentSlot", slot.contentSlotId()),
                                new MarketingContentModels.MarketingObjectRef("TopicContentBlock", topic.topicBlockId())
                        )
                )
        );

        assertThat(resolved).hasSize(3);
        assertThat(resolved).allSatisfy(item -> {
            assertThat(item.publishReady()).isTrue();
            assertThat(item.publicationStatus()).isEqualTo("LIVE");
        });
    }

    @Test
    void schedulerPromotesScheduledToLiveAndThenExpires() throws Exception {
        String ownerId = "merchant-" + UUID.randomUUID();
        MarketingContentModels.ContentSlotView slot = marketingContentService.createContentSlot(
                new MarketingContentModels.CreateContentSlotCommand(
                        "SCHEDULE_SLOT_" + UUID.randomUUID(),
                        "NOTICE",
                        "MERCHANT",
                        ownerId,
                        Map.of("zh-CN", "排期内容", "en-US", "Scheduled Slot"),
                        Map.of("zh-CN", "文案", "en-US", "Body"),
                        List.of(),
                        Map.of("targetType", "URL", "targetValue", "https://gmall.example.com/schedule"),
                        Map.of("scheduleType", "ALWAYS_ON"),
                        "operator-1"
                )
        );

        OffsetDateTime from = OffsetDateTime.now().plusSeconds(1);
        OffsetDateTime to = OffsetDateTime.now().plusSeconds(2);
        MarketingContentModels.ContentSlotView scheduled = marketingContentService.scheduleContentSlot(
                slot.contentSlotId(),
                new MarketingContentModels.ChangeObjectStatusCommand(
                        "operator-2",
                        "排期",
                        Map.of(
                                "scheduleType", "TIMEBOXED",
                                "effectiveFrom", from.toString(),
                                "effectiveTo", to.toString(),
                                "timezone", "Asia/Shanghai"
                        )
                )
        );

        assertThat(scheduled.slotStatus()).isEqualTo("SCHEDULED");

        Thread.sleep(1300);
        marketingContentService.applyPublicationSchedule();
        assertThat(marketingContentService.getContentSlot(slot.contentSlotId()).slotStatus()).isEqualTo("LIVE");

        Thread.sleep(1200);
        marketingContentService.applyPublicationSchedule();
        assertThat(marketingContentService.getContentSlot(slot.contentSlotId()).slotStatus()).isEqualTo("EXPIRED");
    }

    @Test
    void goLiveRejectsWhenRequiredTranslationMissing() {
        String ownerId = "merchant-" + UUID.randomUUID();
        MarketingContentModels.CampaignBannerView banner = marketingContentService.createCampaignBanner(
                new MarketingContentModels.CreateCampaignBannerCommand(
                        "CAMP-MISSING-" + UUID.randomUUID(),
                        "PLATFORM_HOME",
                        "MERCHANT",
                        ownerId,
                        Map.of("zh-CN", "只有中文"),
                        Map.of(),
                        Map.of("url", "https://img.example.com/banner.png"),
                        Map.of("targetType", "URL", "targetValue", "https://gmall.example.com/banner"),
                        Map.of("scheduleType", "ALWAYS_ON"),
                        "operator-1"
                )
        );

        assertThatThrownBy(() -> marketingContentService.goLiveCampaignBanner(
                banner.campaignId(),
                new MarketingContentModels.ChangeObjectStatusCommand("operator-2", "上线", Map.of("scheduleType", "ALWAYS_ON"))
        ))
                .isInstanceOf(MarketingContentRejectedException.class)
                .hasMessageContaining("关键标题缺少双语文案");
    }

    @Test
    void projectionsReturnHomeAndTopicObjects() {
        String ownerId = "merchant-" + UUID.randomUUID();

        MarketingContentModels.CampaignBannerView banner = marketingContentService.createCampaignBanner(
                new MarketingContentModels.CreateCampaignBannerCommand(
                        "CAMP-PROJECTION-" + UUID.randomUUID(),
                        "PLATFORM_HOME",
                        "MERCHANT",
                        ownerId,
                        Map.of("zh-CN", "首页活动", "en-US", "Home Campaign"),
                        Map.of("zh-CN", "副标题", "en-US", "Sub Title"),
                        Map.of("url", "https://img.example.com/banner.png"),
                        Map.of("targetType", "URL", "targetValue", "https://gmall.example.com/banner"),
                        Map.of("scheduleType", "ALWAYS_ON"),
                        "operator-1"
                )
        );
        marketingContentService.goLiveCampaignBanner(
                banner.campaignId(),
                new MarketingContentModels.ChangeObjectStatusCommand("operator-2", "上线", Map.of("scheduleType", "ALWAYS_ON"))
        );

        MarketingContentModels.TopicContentBlockView topic = marketingContentService.createTopicContentBlock(
                new MarketingContentModels.CreateTopicContentBlockCommand(
                        "TOPIC-PROJECTION-" + UUID.randomUUID(),
                        "PLATFORM_TOPIC",
                        "MERCHANT",
                        ownerId,
                        Map.of("zh-CN", "专题标题", "en-US", "Topic Title"),
                        Map.of("zh-CN", "专题摘要", "en-US", "Topic Summary"),
                        Map.of("url", "https://img.example.com/topic.png"),
                        List.of(Map.of("blockType", "TEXT", "value", "hello")),
                        Map.of("targetType", "URL", "targetValue", "https://gmall.example.com/topic"),
                        Map.of("scheduleType", "ALWAYS_ON"),
                        "operator-1"
                )
        );
        marketingContentService.goLiveTopicContentBlock(
                topic.topicBlockId(),
                new MarketingContentModels.ChangeObjectStatusCommand("operator-2", "上线", Map.of("scheduleType", "ALWAYS_ON"))
        );

        MarketingContentModels.ProjectionResultView homeProjection = marketingContentService.getMarketingProjections(
                new MarketingContentModels.ProjectionQueryView("HOME", "MERCHANT", ownerId, "MOBILE", "en-US", "HOME_PAGE")
        );
        MarketingContentModels.ProjectionResultView topicProjection = marketingContentService.getMarketingProjections(
                new MarketingContentModels.ProjectionQueryView("TOPIC", "MERCHANT", ownerId, "MOBILE", "en-US", "TOPIC_PAGE")
        );

        assertThat(homeProjection.items()).isNotEmpty();
        assertThat(homeProjection.items()).anySatisfy(item -> assertThat(item.get("objectType")).isEqualTo("MarketingCampaignBanner"));
        assertThat(topicProjection.items()).isNotEmpty();
        assertThat(topicProjection.items()).allSatisfy(item -> assertThat(item.get("objectType")).isEqualTo("TopicContentBlock"));
    }
}
