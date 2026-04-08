package com.gmall.marketingcontent.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "topic_content_block")
public class TopicContentBlockEntity {

    @Id
    private String topicBlockId;

    @Column(nullable = false, length = 64)
    private String topicCode;

    @Column(nullable = false, length = 32)
    private String topicType;

    @Column(nullable = false, length = 32)
    private String ownerType;

    @Column(nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, length = 4000)
    private String topicTitleI18n;

    @Column(nullable = false, length = 4000)
    private String topicSummaryI18n;

    @Column(nullable = false, length = 4000)
    private String heroImage;

    @Column(nullable = false, length = 8000)
    private String contentBlocks;

    @Column(nullable = false, length = 4000)
    private String landingTarget;

    @Column(nullable = false, length = 4000)
    private String publicationWindow;

    @Column(nullable = false, length = 16)
    private String topicStatus;

    @Column(nullable = false)
    private long version;

    @Column(nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false, length = 64)
    private String updatedBy;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected TopicContentBlockEntity() {
    }

    public TopicContentBlockEntity(String topicBlockId,
                                   String topicCode,
                                   String topicType,
                                   String ownerType,
                                   String ownerId,
                                   String topicTitleI18n,
                                   String topicSummaryI18n,
                                   String heroImage,
                                   String contentBlocks,
                                   String landingTarget,
                                   String publicationWindow,
                                   String topicStatus,
                                   long version,
                                   String createdBy,
                                   OffsetDateTime createdAt,
                                   String updatedBy,
                                   OffsetDateTime updatedAt) {
        this.topicBlockId = topicBlockId;
        this.topicCode = topicCode;
        this.topicType = topicType;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.topicTitleI18n = topicTitleI18n;
        this.topicSummaryI18n = topicSummaryI18n;
        this.heroImage = heroImage;
        this.contentBlocks = contentBlocks;
        this.landingTarget = landingTarget;
        this.publicationWindow = publicationWindow;
        this.topicStatus = topicStatus;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public String getTopicBlockId() {
        return topicBlockId;
    }

    public String getTopicCode() {
        return topicCode;
    }

    public String getTopicType() {
        return topicType;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getTopicTitleI18n() {
        return topicTitleI18n;
    }

    public String getTopicSummaryI18n() {
        return topicSummaryI18n;
    }

    public String getHeroImage() {
        return heroImage;
    }

    public String getContentBlocks() {
        return contentBlocks;
    }

    public String getLandingTarget() {
        return landingTarget;
    }

    public String getPublicationWindow() {
        return publicationWindow;
    }

    public String getTopicStatus() {
        return topicStatus;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String topicTitleI18n,
                       String topicSummaryI18n,
                       String heroImage,
                       String contentBlocks,
                       String landingTarget,
                       String publicationWindow,
                       String operatorId,
                       OffsetDateTime updatedAt) {
        this.topicTitleI18n = topicTitleI18n;
        this.topicSummaryI18n = topicSummaryI18n;
        this.heroImage = heroImage;
        this.contentBlocks = contentBlocks;
        this.landingTarget = landingTarget;
        this.publicationWindow = publicationWindow;
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void schedule(String publicationWindow, String operatorId, OffsetDateTime updatedAt) {
        this.publicationWindow = publicationWindow;
        this.topicStatus = "SCHEDULED";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void goLive(String operatorId, OffsetDateTime updatedAt) {
        this.topicStatus = "LIVE";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void expire(String operatorId, OffsetDateTime updatedAt) {
        this.topicStatus = "EXPIRED";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void offline(String operatorId, OffsetDateTime updatedAt) {
        this.topicStatus = "OFFLINE";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }
}
