package com.gmall.marketingcontent.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class MarketingContentModels {

    private MarketingContentModels() {
    }

    public record CreateCampaignBannerCommand(String campaignCode,
                                              String campaignType,
                                              String ownerType,
                                              String ownerId,
                                              Map<String, String> title,
                                              Map<String, String> subTitle,
                                              Map<String, Object> bannerImage,
                                              Map<String, Object> landingTarget,
                                              Map<String, Object> publicationWindow,
                                              String operatorId) {
    }

    public record UpdateCampaignBannerCommand(Map<String, String> title,
                                              Map<String, String> subTitle,
                                              Map<String, Object> bannerImage,
                                              Map<String, Object> landingTarget,
                                              Map<String, Object> publicationWindow,
                                              String operatorId) {
    }

    public record CampaignBannerView(String campaignId,
                                     String campaignCode,
                                     String campaignType,
                                     String ownerType,
                                     String ownerId,
                                     Map<String, String> title,
                                     Map<String, String> subTitle,
                                     Map<String, Object> bannerImage,
                                     Map<String, Object> landingTarget,
                                     Map<String, Object> publicationWindow,
                                     String campaignStatus,
                                     long version,
                                     OffsetDateTime createdAt,
                                     OffsetDateTime updatedAt) {
    }

    public record CreateContentSlotCommand(String slotCode,
                                           String slotType,
                                           String ownerType,
                                           String ownerId,
                                           Map<String, String> title,
                                           Map<String, String> body,
                                           List<String> mediaList,
                                           Map<String, Object> landingTarget,
                                           Map<String, Object> publicationWindow,
                                           String operatorId) {
    }

    public record UpdateContentSlotCommand(Map<String, String> title,
                                           Map<String, String> body,
                                           List<String> mediaList,
                                           Map<String, Object> landingTarget,
                                           Map<String, Object> publicationWindow,
                                           String operatorId) {
    }

    public record ContentSlotView(String contentSlotId,
                                  String slotCode,
                                  String slotType,
                                  String ownerType,
                                  String ownerId,
                                  Map<String, String> title,
                                  Map<String, String> body,
                                  List<String> mediaList,
                                  Map<String, Object> landingTarget,
                                  Map<String, Object> publicationWindow,
                                  String slotStatus,
                                  long version,
                                  OffsetDateTime createdAt,
                                  OffsetDateTime updatedAt) {
    }

    public record CreateTopicContentBlockCommand(String topicCode,
                                                 String topicType,
                                                 String ownerType,
                                                 String ownerId,
                                                 Map<String, String> topicTitle,
                                                 Map<String, String> topicSummary,
                                                 Map<String, Object> heroImage,
                                                 List<Map<String, Object>> contentBlocks,
                                                 Map<String, Object> landingTarget,
                                                 Map<String, Object> publicationWindow,
                                                 String operatorId) {
    }

    public record UpdateTopicContentBlockCommand(Map<String, String> topicTitle,
                                                 Map<String, String> topicSummary,
                                                 Map<String, Object> heroImage,
                                                 List<Map<String, Object>> contentBlocks,
                                                 Map<String, Object> landingTarget,
                                                 Map<String, Object> publicationWindow,
                                                 String operatorId) {
    }

    public record TopicContentBlockView(String topicBlockId,
                                        String topicCode,
                                        String topicType,
                                        String ownerType,
                                        String ownerId,
                                        Map<String, String> topicTitle,
                                        Map<String, String> topicSummary,
                                        Map<String, Object> heroImage,
                                        List<Map<String, Object>> contentBlocks,
                                        Map<String, Object> landingTarget,
                                        Map<String, Object> publicationWindow,
                                        String topicStatus,
                                        long version,
                                        OffsetDateTime createdAt,
                                        OffsetDateTime updatedAt) {
    }

    public record ChangeObjectStatusCommand(String operatorId,
                                            String comment,
                                            Map<String, Object> publicationWindow) {
    }

    public record MarketingObjectRef(String objectType, String objectId) {
    }

    public record OwnerContext(String ownerType, String ownerId) {
    }

    public record ResolveMarketingObjectsCommand(String locale,
                                                 String resolveMode,
                                                 OwnerContext ownerContext,
                                                 List<MarketingObjectRef> objectRefs) {
    }

    public record ResolvedMarketingObjectView(String objectId,
                                              String objectType,
                                              String resolvedLocale,
                                              boolean fallbackApplied,
                                              String publicationStatus,
                                              boolean publishReady,
                                              Map<String, Object> payload) {
    }

    public record ProjectionQueryView(String projectionType,
                                      String ownerType,
                                      String ownerId,
                                      String terminalType,
                                      String locale,
                                      String pageContext) {
    }

    public record ProjectionResultView(List<Map<String, Object>> items,
                                       String resolvedLocale,
                                       boolean fallbackApplied,
                                       int cacheTtlSeconds) {
    }
}
