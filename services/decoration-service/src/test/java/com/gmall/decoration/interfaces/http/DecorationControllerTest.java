package com.gmall.decoration.interfaces.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmall.decoration.application.DecorationModels;
import com.gmall.decoration.application.DecorationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DecorationControllerTest {

    private final DecorationService decorationService = mock(DecorationService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DecorationController(decorationService))
                .setControllerAdvice(new DecorationHttpExceptionHandler())
                .build();
    }

    @Test
    void createPageReturnsCreatedView() throws Exception {
        when(decorationService.createPage(any(), any()))
                .thenReturn(new DecorationModels.PageView(
                        "page-1",
                        "store-1",
                        "HOME_MAIN",
                        "STOREFRONT_HOME",
                        "MOBILE",
                        "店铺首页",
                        "DRAFTING"
                ));

        mockMvc.perform(post("/api/decoration/admin/storefronts/store-1/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageCode": "HOME_MAIN",
                                  "pageType": "STOREFRONT_HOME",
                                  "terminalType": "MOBILE",
                                  "pageName": "店铺首页",
                                  "operatorId": "tester"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value("page-1"))
                .andExpect(jsonPath("$.pageStatus").value("DRAFTING"));
    }

    @Test
    void listPagesReturnsPageViews() throws Exception {
        when(decorationService.listPages("store-1", "MOBILE", "STOREFRONT_HOME"))
                .thenReturn(List.of(new DecorationModels.PageView(
                        "page-1",
                        "store-1",
                        "HOME_MAIN",
                        "STOREFRONT_HOME",
                        "MOBILE",
                        "店铺首页",
                        "PUBLISHED"
                )));

        mockMvc.perform(get("/api/decoration/admin/storefronts/store-1/pages")
                        .param("terminalType", "MOBILE")
                        .param("pageType", "STOREFRONT_HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pageId").value("page-1"))
                .andExpect(jsonPath("$[0].terminalType").value("MOBILE"));
    }

    @Test
    void createDraftReturnsDraftView() throws Exception {
        when(decorationService.createDraft(any(), any(), any()))
                .thenReturn(new DecorationModels.DraftView(
                        "draft-1",
                        "page-1",
                        1,
                        "EDITING",
                        "PENDING"
                ));

        mockMvc.perform(post("/api/decoration/admin/storefronts/store-1/pages/page-1/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutConfig": {"layout": "hero"},
                                  "componentTree": {"components": []},
                                  "localeContentMap": {"zh-CN": {"title": "店铺首页"}},
                                  "themeConfig": {"theme": "spring"},
                                  "navigationConfig": {"topNav": []},
                                  "operatorId": "tester"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId").value("draft-1"))
                .andExpect(jsonPath("$.draftStatus").value("EDITING"));
    }

    @Test
    void publishReturnsSnapshotView() throws Exception {
        when(decorationService.publish(any(), any(), any()))
                .thenReturn(new DecorationModels.PublishView(
                        "page-1",
                        "draft-1",
                        "snap-1",
                        1,
                        "MOBILE",
                        "PUBLISHED",
                        OffsetDateTime.parse("2026-04-08T14:10:00+08:00")
                ));

        mockMvc.perform(post("/api/decoration/admin/storefronts/store-1/pages/page-1/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": "draft-1",
                                  "reviewId": "review-1",
                                  "targetTerminalType": "MOBILE",
                                  "operationRequestId": "req-publish-1",
                                  "operatorId": "publisher-1",
                                  "publishComment": "发布"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId").value("snap-1"))
                .andExpect(jsonPath("$.pageStatus").value("PUBLISHED"));
    }

    @Test
    void getSnapshotReturnsInternalView() throws Exception {
        when(decorationService.getPublishedSnapshot("store-1", "page-1", "MOBILE", null))
                .thenReturn(new DecorationModels.PublishedSnapshotView(
                        "page-1",
                        "store-1",
                        "STOREFRONT_HOME",
                        "MOBILE",
                        "snap-1",
                        2,
                        Map.of("layoutConfig", Map.of("layout", "hero")),
                        OffsetDateTime.parse("2026-04-08T14:10:00+08:00")
                ));

        mockMvc.perform(get("/api/decoration/internal/storefronts/store-1/decorations/pages/page-1/snapshot")
                        .param("terminalType", "MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId").value("snap-1"))
                .andExpect(jsonPath("$.snapshotVersion").value(2))
                .andExpect(jsonPath("$.publishedPayload.layoutConfig.layout").value("hero"));
    }

    @Test
    void verifySnapshotReturnsChecksumVerificationView() throws Exception {
        when(decorationService.verifyPublishedSnapshot("store-1", "page-1", "MOBILE", null, "expected-1"))
                .thenReturn(new DecorationModels.SnapshotVerificationView(
                        "page-1",
                        "store-1",
                        "MOBILE",
                        "snap-1",
                        2,
                        "real-checksum",
                        "expected-1",
                        false,
                        "CHECKSUM_MISMATCH"
                ));

        mockMvc.perform(get("/api/decoration/internal/storefronts/store-1/decorations/pages/page-1/snapshot/verification")
                        .param("terminalType", "MOBILE")
                        .param("expectedChecksum", "expected-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId").value("snap-1"))
                .andExpect(jsonPath("$.checksumMatched").value(false))
                .andExpect(jsonPath("$.verificationStatus").value("CHECKSUM_MISMATCH"));
    }

    @Test
    void updateDraftReturnsDraftView() throws Exception {
        when(decorationService.updateDraft(any(), any(), any(), any()))
                .thenReturn(new DecorationModels.DraftView(
                        "draft-1",
                        "page-1",
                        1,
                        "EDITING",
                        "PENDING"
                ));

        mockMvc.perform(put("/api/decoration/admin/storefronts/store-1/pages/page-1/drafts/draft-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutConfig": {"layout": "hero"},
                                  "componentTree": {"components": []},
                                  "localeContentMap": {"zh-CN": {"title": "已更新"}},
                                  "themeConfig": {"theme": "spring"},
                                  "navigationConfig": {"topNav": []},
                                  "operatorId": "tester"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId").value("draft-1"));
    }
}
