package com.gmall.decoration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "decoration_draft")
public class DecorationDraftEntity {

    @Id
    private String draftId;

    @Column(nullable = false)
    private String pageId;

    @Column(nullable = false)
    private int draftVersion;

    @Column(nullable = false)
    private String draftStatus;

    @Column(nullable = false, length = 8000)
    private String layoutConfig;

    @Column(nullable = false, length = 8000)
    private String componentTree;

    @Column(nullable = false, length = 8000)
    private String localeContentMap;

    @Column(nullable = false, length = 4000)
    private String themeConfig;

    @Column(nullable = false, length = 4000)
    private String navigationConfig;

    @Column(nullable = false)
    private String validationStatus;

    private OffsetDateTime submittedAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private String updatedBy;

    protected DecorationDraftEntity() {
    }

    public DecorationDraftEntity(String draftId,
                                 String pageId,
                                 int draftVersion,
                                 String draftStatus,
                                 String layoutConfig,
                                 String componentTree,
                                 String localeContentMap,
                                 String themeConfig,
                                 String navigationConfig,
                                 String validationStatus,
                                 OffsetDateTime submittedAt,
                                 OffsetDateTime updatedAt,
                                 String updatedBy) {
        this.draftId = draftId;
        this.pageId = pageId;
        this.draftVersion = draftVersion;
        this.draftStatus = draftStatus;
        this.layoutConfig = layoutConfig;
        this.componentTree = componentTree;
        this.localeContentMap = localeContentMap;
        this.themeConfig = themeConfig;
        this.navigationConfig = navigationConfig;
        this.validationStatus = validationStatus;
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getPageId() {
        return pageId;
    }

    public int getDraftVersion() {
        return draftVersion;
    }

    public String getDraftStatus() {
        return draftStatus;
    }

    public String getLayoutConfig() {
        return layoutConfig;
    }

    public String getComponentTree() {
        return componentTree;
    }

    public String getLocaleContentMap() {
        return localeContentMap;
    }

    public String getThemeConfig() {
        return themeConfig;
    }

    public String getNavigationConfig() {
        return navigationConfig;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void update(String layoutConfig,
                       String componentTree,
                       String localeContentMap,
                       String themeConfig,
                       String navigationConfig,
                       String updatedBy,
                       OffsetDateTime updatedAt) {
        this.layoutConfig = layoutConfig;
        this.componentTree = componentTree;
        this.localeContentMap = localeContentMap;
        this.themeConfig = themeConfig;
        this.navigationConfig = navigationConfig;
        this.validationStatus = "PENDING";
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        if ("REJECTED".equals(this.draftStatus)) {
            this.draftStatus = "EDITING";
        }
    }

    public void markSubmitted(String operatorId, OffsetDateTime submittedAt) {
        this.draftStatus = "SUBMITTED";
        this.validationStatus = "PASSED";
        this.submittedAt = submittedAt;
        this.updatedAt = submittedAt;
        this.updatedBy = operatorId;
    }

    public void markApproved(String operatorId, OffsetDateTime updatedAt) {
        this.draftStatus = "APPROVED";
        this.updatedAt = updatedAt;
        this.updatedBy = operatorId;
    }

    public void markRejected(String operatorId, OffsetDateTime updatedAt) {
        this.draftStatus = "REJECTED";
        this.updatedAt = updatedAt;
        this.updatedBy = operatorId;
    }

    public void markPublished(String operatorId, OffsetDateTime updatedAt) {
        this.draftStatus = "PUBLISHED";
        this.updatedAt = updatedAt;
        this.updatedBy = operatorId;
    }
}
