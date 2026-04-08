package com.gmall.decoration.application;

import java.time.OffsetDateTime;
import java.util.Map;

public final class DecorationModels {

    private DecorationModels() {
    }

    public record CreatePageCommand(String pageCode,
                                    String pageType,
                                    String terminalType,
                                    String pageName,
                                    String operatorId) {
    }

    public record CreateDraftCommand(Map<String, Object> layoutConfig,
                                     Map<String, Object> componentTree,
                                     Map<String, Object> localeContentMap,
                                     Map<String, Object> themeConfig,
                                     Map<String, Object> navigationConfig,
                                     String operatorId) {
    }

    public record UpdateDraftCommand(Map<String, Object> layoutConfig,
                                     Map<String, Object> componentTree,
                                     Map<String, Object> localeContentMap,
                                     Map<String, Object> themeConfig,
                                     Map<String, Object> navigationConfig,
                                     String operatorId) {
    }

    public record SubmitReviewCommand(String operatorId) {
    }

    public record ReviewDecisionCommand(String decision,
                                        String reviewerId,
                                        String comment) {
    }

    public record PublishPageCommand(String draftId,
                                     String reviewId,
                                     String targetTerminalType,
                                     String operationRequestId,
                                     String operatorId,
                                     String publishComment) {
    }

    public record PageView(String pageId,
                           String storefrontId,
                           String pageCode,
                           String pageType,
                           String terminalType,
                           String pageName,
                           String pageStatus) {
    }

    public record DraftView(String draftId,
                            String pageId,
                            int draftVersion,
                            String draftStatus,
                            String validationStatus) {
    }

    public record ReviewView(String reviewId,
                             String draftId,
                             String reviewStatus,
                             String reviewerId,
                             String comment) {
    }

    public record PublishView(String pageId,
                              String draftId,
                              String snapshotId,
                              int snapshotVersion,
                              String terminalType,
                              String pageStatus,
                              OffsetDateTime publishedAt) {
    }

    public record PublishedSnapshotView(String pageId,
                                        String storefrontId,
                                        String pageType,
                                        String terminalType,
                                        String snapshotId,
                                        int snapshotVersion,
                                        Map<String, Object> publishedPayload,
                                        OffsetDateTime publishedAt) {
    }

    public record SnapshotVerificationView(String pageId,
                                           String storefrontId,
                                           String terminalType,
                                           String snapshotId,
                                           int snapshotVersion,
                                           String payloadChecksum,
                                           String expectedChecksum,
                                           boolean checksumMatched,
                                           String verificationStatus) {
    }
}
