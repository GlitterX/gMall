package com.gmall.mallbff.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MallPageRenderService {

    private final DecorationSnapshotGateway decorationSnapshotGateway;
    private final MarketingProjectionGateway marketingProjectionGateway;

    public MallPageRenderService(DecorationSnapshotGateway decorationSnapshotGateway,
                                 MarketingProjectionGateway marketingProjectionGateway) {
        this.decorationSnapshotGateway = decorationSnapshotGateway;
        this.marketingProjectionGateway = marketingProjectionGateway;
    }

    public Map<String, Object> renderPage(String storefrontId,
                                          String pageId,
                                          String terminalType,
                                          String locale) {
        DecorationSnapshotGateway.PublishedSnapshotView snapshot =
                decorationSnapshotGateway.getSnapshot(storefrontId, pageId, terminalType);

        String projectionType = resolveProjectionType(snapshot.pageType());
        String ownerType = resolveOwnerType(snapshot.pageType());
        String ownerId = "PLATFORM".equals(ownerType) ? "platform" : storefrontId;

        MarketingProjectionGateway.ProjectionView projection = marketingProjectionGateway.getProjection(
                projectionType,
                ownerType,
                ownerId,
                terminalType,
                locale,
                snapshot.pageType()
        );

        Map<String, Object> renderPayload = new LinkedHashMap<>();
        if (snapshot.publishedPayload() != null) {
            renderPayload.putAll(snapshot.publishedPayload());
        }

        List<Map<String, Object>> banners = new ArrayList<>();
        List<Map<String, Object>> contentSlots = new ArrayList<>();
        List<Map<String, Object>> topics = new ArrayList<>();
        for (Map<String, Object> item : projection.items()) {
            Object type = item.get("objectType");
            Object payload = item.get("payload");
            if (!(payload instanceof Map<?, ?> payloadMap)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> objectPayload = (Map<String, Object>) payloadMap;
            if ("MarketingCampaignBanner".equals(type)) {
                banners.add(objectPayload);
            } else if ("MarketingContentSlot".equals(type)) {
                contentSlots.add(objectPayload);
            } else if ("TopicContentBlock".equals(type)) {
                topics.add(objectPayload);
            }
        }

        if ("HOME".equals(projectionType)) {
            renderPayload.put("banners", banners);
            renderPayload.put("contentSlots", contentSlots);
        } else {
            renderPayload.put("topics", topics);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pageId", snapshot.pageId());
        result.put("storefrontId", snapshot.storefrontId());
        result.put("pageType", snapshot.pageType());
        result.put("terminalType", snapshot.terminalType());
        result.put("snapshotId", snapshot.snapshotId());
        result.put("snapshotVersion", snapshot.snapshotVersion());
        result.put("renderPayload", renderPayload);
        result.put("resolvedLocale", projection.resolvedLocale());
        result.put("fallbackApplied", projection.fallbackApplied());
        return result;
    }

    private String resolveProjectionType(String pageType) {
        if ("PLATFORM_TOPIC".equals(pageType)) {
            return "TOPIC";
        }
        return "HOME";
    }

    private String resolveOwnerType(String pageType) {
        if ("PLATFORM_HOME".equals(pageType) || "PLATFORM_TOPIC".equals(pageType)) {
            return "PLATFORM";
        }
        return "MERCHANT";
    }
}
