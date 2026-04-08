package com.gmall.marketingcontent.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "marketing_campaign_banner")
public class MarketingCampaignBannerEntity {

    @Id
    private String campaignId;

    @Column(nullable = false, length = 64)
    private String campaignCode;

    @Column(nullable = false, length = 32)
    private String campaignType;

    @Column(nullable = false, length = 32)
    private String ownerType;

    @Column(nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, length = 4000)
    private String titleI18n;

    @Column(nullable = false, length = 4000)
    private String subTitleI18n;

    @Column(nullable = false, length = 4000)
    private String bannerImage;

    @Column(nullable = false, length = 4000)
    private String landingTarget;

    @Column(nullable = false, length = 4000)
    private String publicationWindow;

    @Column(nullable = false, length = 16)
    private String campaignStatus;

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

    protected MarketingCampaignBannerEntity() {
    }

    public MarketingCampaignBannerEntity(String campaignId,
                                         String campaignCode,
                                         String campaignType,
                                         String ownerType,
                                         String ownerId,
                                         String titleI18n,
                                         String subTitleI18n,
                                         String bannerImage,
                                         String landingTarget,
                                         String publicationWindow,
                                         String campaignStatus,
                                         long version,
                                         String createdBy,
                                         OffsetDateTime createdAt,
                                         String updatedBy,
                                         OffsetDateTime updatedAt) {
        this.campaignId = campaignId;
        this.campaignCode = campaignCode;
        this.campaignType = campaignType;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.titleI18n = titleI18n;
        this.subTitleI18n = subTitleI18n;
        this.bannerImage = bannerImage;
        this.landingTarget = landingTarget;
        this.publicationWindow = publicationWindow;
        this.campaignStatus = campaignStatus;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public String getCampaignType() {
        return campaignType;
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

    public String getSubTitleI18n() {
        return subTitleI18n;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public String getLandingTarget() {
        return landingTarget;
    }

    public String getPublicationWindow() {
        return publicationWindow;
    }

    public String getCampaignStatus() {
        return campaignStatus;
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
                       String subTitleI18n,
                       String bannerImage,
                       String landingTarget,
                       String publicationWindow,
                       String operatorId,
                       OffsetDateTime updatedAt) {
        this.titleI18n = titleI18n;
        this.subTitleI18n = subTitleI18n;
        this.bannerImage = bannerImage;
        this.landingTarget = landingTarget;
        this.publicationWindow = publicationWindow;
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void schedule(String publicationWindow, String operatorId, OffsetDateTime updatedAt) {
        this.publicationWindow = publicationWindow;
        this.campaignStatus = "SCHEDULED";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void goLive(String operatorId, OffsetDateTime updatedAt) {
        this.campaignStatus = "LIVE";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void expire(String operatorId, OffsetDateTime updatedAt) {
        this.campaignStatus = "EXPIRED";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }

    public void offline(String operatorId, OffsetDateTime updatedAt) {
        this.campaignStatus = "OFFLINE";
        this.version += 1;
        this.updatedBy = operatorId;
        this.updatedAt = updatedAt;
    }
}
