package com.gmall.decoration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "decoration_review_record")
public class DecorationReviewRecordEntity {

    @Id
    private String reviewId;

    @Column(nullable = false)
    private String draftId;

    @Column(nullable = false)
    private String reviewStatus;

    private String reviewerId;

    private String reviewComment;

    private OffsetDateTime reviewedAt;

    protected DecorationReviewRecordEntity() {
    }

    public DecorationReviewRecordEntity(String reviewId,
                                        String draftId,
                                        String reviewStatus,
                                        String reviewerId,
                                        String reviewComment,
                                        OffsetDateTime reviewedAt) {
        this.reviewId = reviewId;
        this.draftId = draftId;
        this.reviewStatus = reviewStatus;
        this.reviewerId = reviewerId;
        this.reviewComment = reviewComment;
        this.reviewedAt = reviewedAt;
    }

    public String getReviewId() {
        return reviewId;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void decideApproved(String reviewerId, String reviewComment, OffsetDateTime reviewedAt) {
        this.reviewStatus = "APPROVED";
        this.reviewerId = reviewerId;
        this.reviewComment = reviewComment;
        this.reviewedAt = reviewedAt;
    }

    public void decideRejected(String reviewerId, String reviewComment, OffsetDateTime reviewedAt) {
        this.reviewStatus = "REJECTED";
        this.reviewerId = reviewerId;
        this.reviewComment = reviewComment;
        this.reviewedAt = reviewedAt;
    }
}
