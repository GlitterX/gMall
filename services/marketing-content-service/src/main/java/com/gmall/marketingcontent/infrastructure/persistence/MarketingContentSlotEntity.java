package com.gmall.marketingcontent.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "marketing_content_slot")
public class MarketingContentSlotEntity {

    @Id
    private String contentSlotId;

    @Column(nullable = false, length = 64)
    private String slotCode;

    @Column(nullable = false, length = 32)
    private String slotType;

    @Column(nullable = false, length = 32)
    private String ownerType;

    @Column(nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, length = 4000)
    private String titleI18n;

    @Column(nullable = false, length = 8000)
    private String bodyI18n;

    @Column(nullable = false, length = 8000)
    private String mediaList;

    @Column(nullable = false, length = 4000)
    private String landingTarget;

    @Column(nullable = false, length = 4000)
    private String publicationWindow;

    @Column(nullable = false, length = 16)
    private String slotStatus;

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

    protected MarketingContentSlotEntity() {
    }

    public MarketingContentSlotEntity(String contentSlotId,
                                      String slotCode,
                                      String slotType,
                                      String ownerType,
                                      String ownerId,
                                      String titleI18n,
                                      String bodyI18n,
                                      String mediaList,
                                      String landingTarget,
                                      String publicationWindow,
                                      String slotStatus,
                                      long version,
                                      String createdBy,
                                      OffsetDateTime createdAt,
                                      String updatedBy,
                                      OffsetDateTime updatedAt) {
        this.contentSlotId = contentSlotId;
        this.slotCode = slotCode;
        this.slotType = slotType;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.titleI18n = titleI18n;
        this.bodyI18n = bodyI18n;
        this.mediaList = mediaList;
        this.landingTarget = landingTarget;
        this.publicationWindow = publicationWindow;
        this.slotStatus = slotStatus;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public String getContentSlotId() {
        return contentSlotId;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public String getSlotType() {
        return slotType;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getTitleI18n() {
        return titleI18n;
    }

    public String getBodyI18n() {
        return bodyI18n;
    }

    public String getMediaList() {
        return mediaList;
    }

    public String getLandingTarget() {
        return landingTarget;
    }

    public String getPublicationWindow() {
        return publicationWindow;
    }

    public String getSlotStatus() {
        return slotStatus;
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

    public void update(String titleI18n,
                       String bodyI18n,
                       String mediaList,
                       String landingTarget,
                       String publicationWindow,
                       String operatorId,
                       OffsetDateTime updatedAt) {
        this.titleI18n = titleI18n;
        this.bodyI18n = bodyI18n;
        this.mediaList = mediaList;
        this.landingTarget = landingTarget;
        this.publicationWindow = publicationWindow;
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void schedule(String publicationWindow, String operatorId, OffsetDateTime updatedAt) {
        this.publicationWindow = publicationWindow;
        this.slotStatus = "SCHEDULED";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void goLive(String operatorId, OffsetDateTime updatedAt) {
        this.slotStatus = "LIVE";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void expire(String operatorId, OffsetDateTime updatedAt) {
        this.slotStatus = "EXPIRED";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void offline(String operatorId, OffsetDateTime updatedAt) {
        this.slotStatus = "OFFLINE";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }
}
