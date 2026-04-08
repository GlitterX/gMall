package com.gmall.decoration.interfaces.http;

import com.gmall.decoration.application.DecorationModels;
import com.gmall.decoration.application.DecorationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/decoration")
public class DecorationController {

    private final DecorationService decorationService;

    public DecorationController(DecorationService decorationService) {
        this.decorationService = decorationService;
    }

    @PostMapping("/admin/storefronts/{storefrontId}/pages")
    public DecorationModels.PageView createPage(@PathVariable String storefrontId,
                                                @RequestBody DecorationModels.CreatePageCommand command) {
        return decorationService.createPage(storefrontId, command);
    }

    @GetMapping("/admin/storefronts/{storefrontId}/pages")
    public List<DecorationModels.PageView> listPages(@PathVariable String storefrontId,
                                                     @RequestParam(required = false) String terminalType,
                                                     @RequestParam(required = false) String pageType) {
        return decorationService.listPages(storefrontId, terminalType, pageType);
    }

    @PostMapping("/admin/storefronts/{storefrontId}/pages/{pageId}/drafts")
    public DecorationModels.DraftView createDraft(@PathVariable String storefrontId,
                                                  @PathVariable String pageId,
                                                  @RequestBody DecorationModels.CreateDraftCommand command) {
        return decorationService.createDraft(storefrontId, pageId, command);
    }

    @PutMapping("/admin/storefronts/{storefrontId}/pages/{pageId}/drafts/{draftId}")
    public DecorationModels.DraftView updateDraft(@PathVariable String storefrontId,
                                                  @PathVariable String pageId,
                                                  @PathVariable String draftId,
                                                  @RequestBody DecorationModels.UpdateDraftCommand command) {
        return decorationService.updateDraft(storefrontId, pageId, draftId, command);
    }

    @PostMapping("/admin/storefronts/{storefrontId}/pages/{pageId}/drafts/{draftId}/submit-review")
    public DecorationModels.ReviewView submitReview(@PathVariable String storefrontId,
                                                    @PathVariable String pageId,
                                                    @PathVariable String draftId,
                                                    @RequestBody DecorationModels.SubmitReviewCommand command) {
        return decorationService.submitReview(storefrontId, pageId, draftId, command.operatorId());
    }

    @PostMapping("/admin/storefronts/{storefrontId}/reviews/{reviewId}/decision")
    public DecorationModels.ReviewView decideReview(@PathVariable String storefrontId,
                                                    @PathVariable String reviewId,
                                                    @RequestBody DecorationModels.ReviewDecisionCommand command) {
        return decorationService.decideReview(storefrontId, reviewId, command);
    }

    @PostMapping("/admin/storefronts/{storefrontId}/pages/{pageId}/publish")
    public DecorationModels.PublishView publish(@PathVariable String storefrontId,
                                                @PathVariable String pageId,
                                                @RequestBody DecorationModels.PublishPageCommand command) {
        return decorationService.publish(storefrontId, pageId, command);
    }

    @GetMapping("/internal/storefronts/{storefrontId}/decorations/pages/{pageId}/snapshot")
    public DecorationModels.PublishedSnapshotView getSnapshot(@PathVariable String storefrontId,
                                                              @PathVariable String pageId,
                                                              @RequestParam String terminalType,
                                                              @RequestParam(required = false) String snapshotId) {
        return decorationService.getPublishedSnapshot(storefrontId, pageId, terminalType, snapshotId);
    }

    @GetMapping("/internal/storefronts/{storefrontId}/decorations/pages/{pageId}/snapshot/verification")
    public DecorationModels.SnapshotVerificationView verifySnapshot(@PathVariable String storefrontId,
                                                                    @PathVariable String pageId,
                                                                    @RequestParam String terminalType,
                                                                    @RequestParam(required = false) String snapshotId,
                                                                    @RequestParam(required = false) String expectedChecksum) {
        return decorationService.verifyPublishedSnapshot(storefrontId, pageId, terminalType, snapshotId, expectedChecksum);
    }
}
