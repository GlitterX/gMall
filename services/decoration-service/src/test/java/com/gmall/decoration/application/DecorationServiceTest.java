package com.gmall.decoration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecorationServiceTest {

    private final DecorationPageRepository pageRepository = mock(DecorationPageRepository.class);
    private final DecorationDraftRepository draftRepository = mock(DecorationDraftRepository.class);
    private final DecorationReviewRecordRepository reviewRecordRepository = mock(DecorationReviewRecordRepository.class);
    private final DecorationSnapshotRepository snapshotRepository = mock(DecorationSnapshotRepository.class);
    private final DecorationPublishRecordRepository publishRecordRepository = mock(DecorationPublishRecordRepository.class);
    private final FoundationStorefrontGateway foundationStorefrontGateway = mock(FoundationStorefrontGateway.class);
    private final CatalogDecorationProjectionGateway catalogDecorationProjectionGateway = mock(CatalogDecorationProjectionGateway.class);
    private final MarketingContentGateway marketingContentGateway = mock(MarketingContentGateway.class);
    private final DecorationEventAppender decorationEventAppender = mock(DecorationEventAppender.class);

    private final List<DecorationPageEntity> savedPages = new ArrayList<>();
    private final List<DecorationDraftEntity> savedDrafts = new ArrayList<>();
    private final List<DecorationReviewRecordEntity> savedReviews = new ArrayList<>();
    private final List<DecorationSnapshotEntity> savedSnapshots = new ArrayList<>();
    private final List<DecorationPublishRecordEntity> savedPublishRecords = new ArrayList<>();

    private DecorationService decorationService;

    @BeforeEach
    void setUp() {
        decorationService = new DecorationService(
                pageRepository,
                draftRepository,
                reviewRecordRepository,
                snapshotRepository,
                publishRecordRepository,
                foundationStorefrontGateway,
                catalogDecorationProjectionGateway,
                marketingContentGateway,
                decorationEventAppender,
                new ObjectMapper()
        );
        when(pageRepository.save(any(DecorationPageEntity.class))).thenAnswer(invocation -> {
            DecorationPageEntity entity = invocation.getArgument(0);
            savedPages.add(entity);
            return entity;
        });
        when(pageRepository.findByStorefrontIdAndPageCodeAndTerminalType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(draftRepository.save(any(DecorationDraftEntity.class))).thenAnswer(invocation -> {
            DecorationDraftEntity entity = invocation.getArgument(0);
            savedDrafts.add(entity);
            return entity;
        });
        when(reviewRecordRepository.save(any(DecorationReviewRecordEntity.class))).thenAnswer(invocation -> {
            DecorationReviewRecordEntity entity = invocation.getArgument(0);
            savedReviews.add(entity);
            return entity;
        });
        when(snapshotRepository.save(any(DecorationSnapshotEntity.class))).thenAnswer(invocation -> {
            DecorationSnapshotEntity entity = invocation.getArgument(0);
            savedSnapshots.add(entity);
            return entity;
        });
        when(publishRecordRepository.save(any(DecorationPublishRecordEntity.class))).thenAnswer(invocation -> {
            DecorationPublishRecordEntity entity = invocation.getArgument(0);
            savedPublishRecords.add(entity);
            return entity;
        });
    }

    @Test
    void createPageRejectsPlatformHomeForMerchantStorefront() {
        when(foundationStorefrontGateway.getStorefront("store-merchant"))
                .thenReturn(new FoundationStorefrontGateway.StorefrontView(
                        "store-merchant",
                        "MERCHANT",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US"
                ));

        assertThatThrownBy(() -> decorationService.createPage(
                "store-merchant",
                new DecorationModels.CreatePageCommand(
                        "HOME_MAIN",
                        "PLATFORM_HOME",
                        "MOBILE",
                        "平台首页",
                        "tester"
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("页面类型不允许归属该店铺");
    }

    @Test
    void createDraftSubmitReviewApproveAndPublishPersistSnapshot() {
        DecorationPageEntity page = new DecorationPageEntity(
                "page-1",
                "store-1",
                "HOME_MAIN",
                "STOREFRONT_HOME",
                "MOBILE",
                "店铺首页",
                "DRAFTING",
                null,
                OffsetDateTime.parse("2026-04-08T14:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T14:00:00+08:00")
        );
        when(foundationStorefrontGateway.getStorefront("store-1"))
                .thenReturn(new FoundationStorefrontGateway.StorefrontView(
                        "store-1",
                        "MERCHANT",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US"
                ));
        when(foundationStorefrontGateway.getOperability("store-1", "MOBILE", "EDIT"))
                .thenReturn(operability(true));
        when(foundationStorefrontGateway.getOperability("store-1", "MOBILE", "SUBMIT"))
                .thenReturn(operability(true));
        when(foundationStorefrontGateway.getOperability("store-1", "MOBILE", "PUBLISH"))
                .thenReturn(operability(true));
        when(pageRepository.findById("page-1")).thenReturn(Optional.of(page));
        when(draftRepository.findFirstByPageIdAndDraftStatusInOrderByDraftVersionDesc(
                "page-1",
                List.of("EDITING", "SUBMITTED", "APPROVED", "REJECTED")
        )).thenReturn(Optional.empty());
        when(draftRepository.findFirstByPageIdOrderByDraftVersionDesc("page-1")).thenReturn(Optional.empty());
        when(draftRepository.findById(any())).thenAnswer(invocation -> {
            String draftId = invocation.getArgument(0);
            return savedDrafts.stream().filter(draftEntity -> draftEntity.getDraftId().equals(draftId)).findFirst();
        });
        when(reviewRecordRepository.findById(any())).thenAnswer(invocation -> {
            String reviewId = invocation.getArgument(0);
            return savedReviews.stream().filter(reviewEntity -> reviewEntity.getReviewId().equals(reviewId)).findFirst();
        });
        when(snapshotRepository.findFirstByPageIdAndTerminalTypeOrderBySnapshotVersionDesc("page-1", "MOBILE"))
                .thenReturn(Optional.empty());
        when(publishRecordRepository.findByOperationRequestId("req-publish-1")).thenReturn(Optional.empty());
        when(catalogDecorationProjectionGateway.getProducts(List.of("view-1"), "zh-CN"))
                .thenReturn(List.of(new CatalogDecorationProjectionGateway.DecorationProduct(
                        "view-1",
                        "有机草莓礼盒",
                        "https://img.example.com/1.png",
                        "ACTIVE",
                        "SUPPLY",
                        "zh-CN",
                        false,
                        "COMPLETE"
                )));
        when(marketingContentGateway.resolveObjects(
                List.of(new MarketingContentGateway.MarketingObjectRef("MarketingContentSlot", "slot-1")),
                "zh-CN",
                "PUBLISH_VALIDATE",
                "MERCHANT",
                "store-1"
        )).thenReturn(List.of(
                new MarketingContentGateway.ResolvedMarketingObject(
                        "slot-1",
                        "MarketingContentSlot",
                        "zh-CN",
                        false,
                        "LIVE",
                        true,
                        Map.of("slotCode", "HOME_HERO")
                )
        ));

        DecorationModels.DraftView draftView = decorationService.createDraft(
                "store-1",
                "page-1",
                new DecorationModels.CreateDraftCommand(
                        Map.of("layout", "hero"),
                        Map.of("components", List.of(
                                Map.of("componentType", "PRODUCT_CARD", "productViewId", "view-1"),
                                Map.of("componentType", "MARKETING_CONTENT_SLOT", "contentSlotId", "slot-1")
                        )),
                        Map.of("zh-CN", Map.of("title", "店铺首页")),
                        Map.of("theme", "spring"),
                        Map.of("topNav", List.of()),
                        "tester"
                )
        );
        DecorationModels.ReviewView reviewView = decorationService.submitReview("store-1", "page-1", draftView.draftId(), "tester");
        DecorationModels.ReviewView approvedView = decorationService.decideReview(
                "store-1",
                reviewView.reviewId(),
                new DecorationModels.ReviewDecisionCommand("APPROVED", "reviewer-1", "通过")
        );
        DecorationModels.PublishView publishView = decorationService.publish(
                "store-1",
                "page-1",
                new DecorationModels.PublishPageCommand(draftView.draftId(), reviewView.reviewId(), "MOBILE", "req-publish-1", "publisher-1", "发布")
        );

        assertThat(draftView.draftStatus()).isEqualTo("EDITING");
        assertThat(reviewView.reviewStatus()).isEqualTo("SUBMITTED");
        assertThat(approvedView.reviewStatus()).isEqualTo("APPROVED");
        assertThat(publishView.pageStatus()).isEqualTo("PUBLISHED");
        assertThat(savedSnapshots).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.getPageId()).isEqualTo("page-1");
            assertThat(snapshot.getTerminalType()).isEqualTo("MOBILE");
            assertThat(snapshot.getSnapshotVersion()).isEqualTo(1);
            assertThat(snapshot.getPublishedPayload()).contains("resolvedMarketingObjects");
        });
        assertThat(savedPublishRecords).singleElement().satisfies(record -> {
            assertThat(record.getOperationRequestId()).isEqualTo("req-publish-1");
            assertThat(record.getOperationStatus()).isEqualTo("PUBLISHED");
        });
    }

    @Test
    void publishRejectsWhenDecorationProductsAreMissing() {
        DecorationPageEntity page = new DecorationPageEntity(
                "page-2",
                "store-2",
                "HOME_MAIN",
                "STOREFRONT_HOME",
                "MOBILE",
                "店铺首页",
                "DRAFTING",
                null,
                OffsetDateTime.parse("2026-04-08T14:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T14:00:00+08:00")
        );
        DecorationDraftEntity draft = new DecorationDraftEntity(
                "draft-2",
                "page-2",
                1,
                "APPROVED",
                "{\"layout\":\"hero\"}",
                "{\"components\":[{\"componentType\":\"PRODUCT_CARD\",\"productViewId\":\"view-missing\"}]}",
                "{\"zh-CN\":{\"title\":\"店铺首页\"}}",
                "{\"theme\":\"spring\"}",
                "{\"topNav\":[]}",
                "PASSED",
                OffsetDateTime.parse("2026-04-08T14:05:00+08:00"),
                OffsetDateTime.parse("2026-04-08T14:05:00+08:00"),
                "tester"
        );
        when(pageRepository.findById("page-2")).thenReturn(Optional.of(page));
        when(draftRepository.findById("draft-2")).thenReturn(Optional.of(draft));
        when(reviewRecordRepository.findById("review-2")).thenReturn(Optional.of(new DecorationReviewRecordEntity(
                "review-2",
                "draft-2",
                "APPROVED",
                "reviewer-2",
                "通过",
                OffsetDateTime.parse("2026-04-08T14:06:00+08:00")
        )));
        when(foundationStorefrontGateway.getOperability("store-2", "MOBILE", "PUBLISH"))
                .thenReturn(operability(true));
        when(foundationStorefrontGateway.getStorefront("store-2"))
                .thenReturn(new FoundationStorefrontGateway.StorefrontView(
                        "store-2",
                        "MERCHANT",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US"
                ));
        when(publishRecordRepository.findByOperationRequestId("req-publish-2")).thenReturn(Optional.empty());
        when(catalogDecorationProjectionGateway.getProducts(List.of("view-missing"), "zh-CN"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> decorationService.publish(
                "store-2",
                "page-2",
                new DecorationModels.PublishPageCommand("draft-2", "review-2", "MOBILE", "req-publish-2", "publisher-2", "发布")
        ))
                .isInstanceOf(DecorationRejectedException.class)
                .hasMessageContaining("商品投影不可用");
    }

    @Test
    void publishRejectsWhenMarketingObjectValidationFails() {
        DecorationPageEntity page = new DecorationPageEntity(
                "page-3",
                "store-3",
                "HOME_MAIN",
                "STOREFRONT_HOME",
                "MOBILE",
                "店铺首页",
                "DRAFTING",
                null,
                OffsetDateTime.parse("2026-04-08T14:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T14:00:00+08:00")
        );
        DecorationDraftEntity draft = new DecorationDraftEntity(
                "draft-3",
                "page-3",
                1,
                "APPROVED",
                "{\"layout\":\"hero\"}",
                "{\"components\":[{\"componentType\":\"MARKETING_CONTENT_SLOT\",\"contentSlotId\":\"slot-missing\"}]}",
                "{\"zh-CN\":{\"title\":\"店铺首页\"}}",
                "{\"theme\":\"spring\"}",
                "{\"topNav\":[]}",
                "PASSED",
                OffsetDateTime.parse("2026-04-08T14:05:00+08:00"),
                OffsetDateTime.parse("2026-04-08T14:05:00+08:00"),
                "tester"
        );
        when(pageRepository.findById("page-3")).thenReturn(Optional.of(page));
        when(draftRepository.findById("draft-3")).thenReturn(Optional.of(draft));
        when(reviewRecordRepository.findById("review-3")).thenReturn(Optional.of(new DecorationReviewRecordEntity(
                "review-3",
                "draft-3",
                "APPROVED",
                "reviewer-3",
                "通过",
                OffsetDateTime.parse("2026-04-08T14:06:00+08:00")
        )));
        when(foundationStorefrontGateway.getOperability("store-3", "MOBILE", "PUBLISH"))
                .thenReturn(operability(true));
        when(foundationStorefrontGateway.getStorefront("store-3"))
                .thenReturn(new FoundationStorefrontGateway.StorefrontView(
                        "store-3",
                        "MERCHANT",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US"
                ));
        when(publishRecordRepository.findByOperationRequestId("req-publish-3")).thenReturn(Optional.empty());
        when(marketingContentGateway.resolveObjects(
                List.of(new MarketingContentGateway.MarketingObjectRef("MarketingContentSlot", "slot-missing")),
                "zh-CN",
                "PUBLISH_VALIDATE",
                "MERCHANT",
                "store-3"
        )).thenReturn(List.of(new MarketingContentGateway.ResolvedMarketingObject(
                "slot-missing",
                "MarketingContentSlot",
                "zh-CN",
                false,
                "DRAFT",
                false,
                Map.of()
        )));

        assertThatThrownBy(() -> decorationService.publish(
                "store-3",
                "page-3",
                new DecorationModels.PublishPageCommand("draft-3", "review-3", "MOBILE", "req-publish-3", "publisher-3", "发布")
        ))
                .isInstanceOf(DecorationRejectedException.class)
                .hasMessageContaining("营销对象不可用");
    }

    @Test
    void createPageRejectsWhenPageAlreadyExistsInSameStorefrontAndTerminal() {
        when(foundationStorefrontGateway.getStorefront("store-dup"))
                .thenReturn(new FoundationStorefrontGateway.StorefrontView(
                        "store-dup",
                        "MERCHANT",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US"
                ));
        when(pageRepository.findByStorefrontIdAndPageCodeAndTerminalType("store-dup", "HOME_MAIN", "MOBILE"))
                .thenReturn(Optional.of(new DecorationPageEntity(
                        "page-existing",
                        "store-dup",
                        "HOME_MAIN",
                        "STOREFRONT_HOME",
                        "MOBILE",
                        "已存在首页",
                        "PUBLISHED",
                        "snapshot-existing",
                        OffsetDateTime.parse("2026-04-08T14:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-08T14:00:00+08:00")
                )));

        assertThatThrownBy(() -> decorationService.createPage(
                "store-dup",
                new DecorationModels.CreatePageCommand(
                        "HOME_MAIN",
                        "STOREFRONT_HOME",
                        "MOBILE",
                        "店铺首页",
                        "tester"
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("页面已存在");
    }

    @Test
    void listPagesReturnsFilteredStorefrontPages() {
        when(pageRepository.findByStorefrontIdOrderByCreatedAtDesc("store-list"))
                .thenReturn(List.of(
                        new DecorationPageEntity(
                                "page-mobile",
                                "store-list",
                                "HOME_MAIN",
                                "STOREFRONT_HOME",
                                "MOBILE",
                                "移动端首页",
                                "PUBLISHED",
                                "snapshot-mobile",
                                OffsetDateTime.parse("2026-04-08T14:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-08T14:00:00+08:00")
                        ),
                        new DecorationPageEntity(
                                "page-pc",
                                "store-list",
                                "HOME_MAIN",
                                "STOREFRONT_HOME",
                                "PC",
                                "PC 首页",
                                "DRAFTING",
                                null,
                                OffsetDateTime.parse("2026-04-08T13:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-08T13:00:00+08:00")
                        )
                ));

        List<DecorationModels.PageView> views = decorationService.listPages("store-list", "MOBILE", "STOREFRONT_HOME");

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.pageId()).isEqualTo("page-mobile");
            assertThat(view.terminalType()).isEqualTo("MOBILE");
        });
    }

    @Test
    void verifyPublishedSnapshotReturnsChecksumMismatchWhenExpectedChecksumDiffers() {
        DecorationPageEntity page = new DecorationPageEntity(
                "page-checksum",
                "store-checksum",
                "HOME_MAIN",
                "STOREFRONT_HOME",
                "MOBILE",
                "店铺首页",
                "PUBLISHED",
                "snapshot-checksum",
                OffsetDateTime.parse("2026-04-08T14:00:00+08:00"),
                OffsetDateTime.parse("2026-04-08T14:00:00+08:00")
        );
        DecorationSnapshotEntity snapshot = new DecorationSnapshotEntity(
                "snapshot-checksum",
                "page-checksum",
                "MOBILE",
                2,
                "draft-checksum",
                "{\"layoutConfig\":{\"layout\":\"hero\"}}",
                "abc123",
                "ACTIVE",
                "publisher-1",
                OffsetDateTime.parse("2026-04-08T14:10:00+08:00")
        );
        when(pageRepository.findById("page-checksum")).thenReturn(Optional.of(page));
        when(snapshotRepository.findById("snapshot-checksum")).thenReturn(Optional.of(snapshot));

        DecorationModels.SnapshotVerificationView view = decorationService.verifyPublishedSnapshot(
                "store-checksum",
                "page-checksum",
                "MOBILE",
                null,
                "mismatch-checksum"
        );

        assertThat(view.snapshotId()).isEqualTo("snapshot-checksum");
        assertThat(view.payloadChecksum()).isEqualTo("abc123");
        assertThat(view.checksumMatched()).isFalse();
        assertThat(view.verificationStatus()).isEqualTo("CHECKSUM_MISMATCH");
    }

    private FoundationStorefrontGateway.StorefrontOperability operability(boolean allowed) {
        return new FoundationStorefrontGateway.StorefrontOperability(
                "store-1",
                "MOBILE",
                "ACTIVE",
                true,
                true,
                true,
                false,
                true,
                "NOT_BOUND",
                false,
                allowed
        );
    }
}
