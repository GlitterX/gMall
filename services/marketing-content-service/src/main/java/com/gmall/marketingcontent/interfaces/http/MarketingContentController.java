package com.gmall.marketingcontent.interfaces.http;

import com.gmall.marketingcontent.application.MarketingContentModels;
import com.gmall.marketingcontent.application.MarketingContentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketing")
public class MarketingContentController {

    private final MarketingContentService marketingContentService;

    public MarketingContentController(MarketingContentService marketingContentService) {
        this.marketingContentService = marketingContentService;
    }

    @PostMapping("/admin/campaign-banners")
    public MarketingContentModels.CampaignBannerView createCampaignBanner(
            @RequestBody MarketingContentModels.CreateCampaignBannerCommand command) {
        return marketingContentService.createCampaignBanner(command);
    }

    @PutMapping("/admin/campaign-banners/{campaignId}")
    public MarketingContentModels.CampaignBannerView updateCampaignBanner(
            @PathVariable String campaignId,
            @RequestBody MarketingContentModels.UpdateCampaignBannerCommand command) {
        return marketingContentService.updateCampaignBanner(campaignId, command);
    }

    @GetMapping("/admin/campaign-banners/{campaignId}")
    public MarketingContentModels.CampaignBannerView getCampaignBanner(@PathVariable String campaignId) {
        return marketingContentService.getCampaignBanner(campaignId);
    }

    @GetMapping("/admin/campaign-banners")
    public List<MarketingContentModels.CampaignBannerView> listCampaignBanners(@RequestParam(required = false) String ownerType,
                                                                                @RequestParam(required = false) String ownerId,
                                                                                @RequestParam(required = false) String campaignStatus) {
        return marketingContentService.listCampaignBanners(ownerType, ownerId, campaignStatus);
    }

    @PostMapping("/admin/campaign-banners/{campaignId}/schedule")
    public MarketingContentModels.CampaignBannerView scheduleCampaignBanner(
            @PathVariable String campaignId,
            @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.scheduleCampaignBanner(campaignId, command);
    }

    @PostMapping("/admin/campaign-banners/{campaignId}/go-live")
    public MarketingContentModels.CampaignBannerView goLiveCampaignBanner(
            @PathVariable String campaignId,
            @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.goLiveCampaignBanner(campaignId, command);
    }

    @PostMapping("/admin/campaign-banners/{campaignId}/offline")
    public MarketingContentModels.CampaignBannerView offlineCampaignBanner(
            @PathVariable String campaignId,
            @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.offlineCampaignBanner(campaignId, command);
    }

    @PostMapping("/admin/content-slots")
    public MarketingContentModels.ContentSlotView createContentSlot(
            @RequestBody MarketingContentModels.CreateContentSlotCommand command) {
        return marketingContentService.createContentSlot(command);
    }

    @PutMapping("/admin/content-slots/{contentSlotId}")
    public MarketingContentModels.ContentSlotView updateContentSlot(
            @PathVariable String contentSlotId,
            @RequestBody MarketingContentModels.UpdateContentSlotCommand command) {
        return marketingContentService.updateContentSlot(contentSlotId, command);
    }

    @GetMapping("/admin/content-slots/{contentSlotId}")
    public MarketingContentModels.ContentSlotView getContentSlot(@PathVariable String contentSlotId) {
        return marketingContentService.getContentSlot(contentSlotId);
    }

    @GetMapping("/admin/content-slots")
    public List<MarketingContentModels.ContentSlotView> listContentSlots(@RequestParam(required = false) String ownerType,
                                                                         @RequestParam(required = false) String ownerId,
                                                                         @RequestParam(required = false) String slotStatus) {
        return marketingContentService.listContentSlots(ownerType, ownerId, slotStatus);
    }

    @PostMapping("/admin/content-slots/{contentSlotId}/schedule")
    public MarketingContentModels.ContentSlotView scheduleContentSlot(@PathVariable String contentSlotId,
                                                                      @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.scheduleContentSlot(contentSlotId, command);
    }

    @PostMapping("/admin/content-slots/{contentSlotId}/go-live")
    public MarketingContentModels.ContentSlotView goLiveContentSlot(@PathVariable String contentSlotId,
                                                                    @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.goLiveContentSlot(contentSlotId, command);
    }

    @PostMapping("/admin/content-slots/{contentSlotId}/offline")
    public MarketingContentModels.ContentSlotView offlineContentSlot(@PathVariable String contentSlotId,
                                                                     @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.offlineContentSlot(contentSlotId, command);
    }

    @PostMapping("/admin/topic-content-blocks")
    public MarketingContentModels.TopicContentBlockView createTopicContentBlock(
            @RequestBody MarketingContentModels.CreateTopicContentBlockCommand command) {
        return marketingContentService.createTopicContentBlock(command);
    }

    @PutMapping("/admin/topic-content-blocks/{topicBlockId}")
    public MarketingContentModels.TopicContentBlockView updateTopicContentBlock(
            @PathVariable String topicBlockId,
            @RequestBody MarketingContentModels.UpdateTopicContentBlockCommand command) {
        return marketingContentService.updateTopicContentBlock(topicBlockId, command);
    }

    @GetMapping("/admin/topic-content-blocks/{topicBlockId}")
    public MarketingContentModels.TopicContentBlockView getTopicContentBlock(@PathVariable String topicBlockId) {
        return marketingContentService.getTopicContentBlock(topicBlockId);
    }

    @GetMapping("/admin/topic-content-blocks")
    public List<MarketingContentModels.TopicContentBlockView> listTopicContentBlocks(@RequestParam(required = false) String ownerType,
                                                                                      @RequestParam(required = false) String ownerId,
                                                                                      @RequestParam(required = false) String topicStatus) {
        return marketingContentService.listTopicContentBlocks(ownerType, ownerId, topicStatus);
    }

    @PostMapping("/admin/topic-content-blocks/{topicBlockId}/schedule")
    public MarketingContentModels.TopicContentBlockView scheduleTopicContentBlock(@PathVariable String topicBlockId,
                                                                                  @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.scheduleTopicContentBlock(topicBlockId, command);
    }

    @PostMapping("/admin/topic-content-blocks/{topicBlockId}/go-live")
    public MarketingContentModels.TopicContentBlockView goLiveTopicContentBlock(@PathVariable String topicBlockId,
                                                                                @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.goLiveTopicContentBlock(topicBlockId, command);
    }

    @PostMapping("/admin/topic-content-blocks/{topicBlockId}/offline")
    public MarketingContentModels.TopicContentBlockView offlineTopicContentBlock(@PathVariable String topicBlockId,
                                                                                 @RequestBody MarketingContentModels.ChangeObjectStatusCommand command) {
        return marketingContentService.offlineTopicContentBlock(topicBlockId, command);
    }

    @PostMapping("/internal/marketing/objects/resolve")
    public List<MarketingContentModels.ResolvedMarketingObjectView> resolveMarketingObjects(
            @RequestBody MarketingContentModels.ResolveMarketingObjectsCommand command) {
        return marketingContentService.resolveMarketingObjects(command);
    }

    @GetMapping("/internal/marketing/projections")
    public MarketingContentModels.ProjectionResultView getMarketingProjections(
            @RequestParam String projectionType,
            @RequestParam String ownerType,
            @RequestParam String ownerId,
            @RequestParam(required = false, defaultValue = "MOBILE") String terminalType,
            @RequestParam(required = false, defaultValue = "zh-CN") String locale,
            @RequestParam(required = false) String pageContext) {
        return marketingContentService.getMarketingProjections(new MarketingContentModels.ProjectionQueryView(
                projectionType,
                ownerType,
                ownerId,
                terminalType,
                locale,
                pageContext
        ));
    }
}
