package com.gmall.marketingcontent.interfaces.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmall.marketingcontent.application.MarketingContentModels;
import com.gmall.marketingcontent.application.MarketingContentService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MarketingContentControllerTest {

    private final MarketingContentService marketingContentService = mock(MarketingContentService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MarketingContentController(marketingContentService))
                .setControllerAdvice(new MarketingContentHttpExceptionHandler())
                .build();
    }

    @Test
    void createCampaignBannerReturnsCreatedView() throws Exception {
        when(marketingContentService.createCampaignBanner(any()))
                .thenReturn(new MarketingContentModels.CampaignBannerView(
                        "camp-1",
                        "HOME_CAMP",
                        "PLATFORM_HOME",
                        "MERCHANT",
                        "merchant-1",
                        Map.of("zh-CN", "首页活动", "en-US", "Home Campaign"),
                        Map.of("zh-CN", "副标题", "en-US", "Sub title"),
                        Map.of("url", "https://img.example.com/banner.png"),
                        Map.of("targetType", "URL", "targetValue", "https://gmall.example.com/banner"),
                        Map.of("scheduleType", "ALWAYS_ON"),
                        "DRAFT",
                        1L,
                        OffsetDateTime.parse("2026-04-08T14:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-08T14:00:00+08:00")
                ));

        mockMvc.perform(post("/api/marketing/admin/campaign-banners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "campaignCode": "HOME_CAMP",
                                  "campaignType": "PLATFORM_HOME",
                                  "ownerType": "MERCHANT",
                                  "ownerId": "merchant-1",
                                  "title": {
                                    "zh-CN": "首页活动",
                                    "en-US": "Home Campaign"
                                  },
                                  "subTitle": {
                                    "zh-CN": "副标题",
                                    "en-US": "Sub title"
                                  },
                                  "bannerImage": {
                                    "url": "https://img.example.com/banner.png"
                                  },
                                  "landingTarget": {
                                    "targetType": "URL",
                                    "targetValue": "https://gmall.example.com/banner"
                                  },
                                  "publicationWindow": {
                                    "scheduleType": "ALWAYS_ON"
                                  },
                                  "operatorId": "operator-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("camp-1"))
                .andExpect(jsonPath("$.campaignStatus").value("DRAFT"));
    }

    @Test
    void resolveMarketingObjectsReturnsResolvedPayload() throws Exception {
        when(marketingContentService.resolveMarketingObjects(any()))
                .thenReturn(List.of(new MarketingContentModels.ResolvedMarketingObjectView(
                        "topic-1",
                        "TopicContentBlock",
                        "en-US",
                        false,
                        "LIVE",
                        true,
                        Map.of(
                                "topicCode", "TOPIC_HOME",
                                "topicTitle", Map.of("value", "Topic title")
                        )
                )));

        mockMvc.perform(post("/api/marketing/internal/marketing/objects/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locale": "en-US",
                                  "resolveMode": "PUBLISH_VALIDATE",
                                  "ownerContext": {
                                    "ownerType": "MERCHANT",
                                    "ownerId": "merchant-1"
                                  },
                                  "objectRefs": [
                                    {
                                      "objectType": "TopicContentBlock",
                                      "objectId": "topic-1"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].objectType").value("TopicContentBlock"))
                .andExpect(jsonPath("$[0].publicationStatus").value("LIVE"));
    }

    @Test
    void getMarketingProjectionsReturnsItems() throws Exception {
        when(marketingContentService.getMarketingProjections(any()))
                .thenReturn(new MarketingContentModels.ProjectionResultView(
                        List.of(Map.of(
                                "objectId", "camp-1",
                                "objectType", "MarketingCampaignBanner"
                        )),
                        "en-US",
                        false,
                        120
                ));

        mockMvc.perform(get("/api/marketing/internal/marketing/projections")
                        .param("projectionType", "HOME")
                        .param("ownerType", "MERCHANT")
                        .param("ownerId", "merchant-1")
                        .param("terminalType", "MOBILE")
                        .param("locale", "en-US")
                        .param("pageContext", "HOME_PAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheTtlSeconds").value(120))
                .andExpect(jsonPath("$.items[0].objectType").value("MarketingCampaignBanner"));
    }
}
