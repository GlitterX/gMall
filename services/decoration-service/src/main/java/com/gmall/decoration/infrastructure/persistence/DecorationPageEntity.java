package com.gmall.decoration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "decoration_page")
public class DecorationPageEntity {

    @Id
    private String pageId;

    @Column(nullable = false)
    private String storefrontId;

    @Column(nullable = false)
    private String pageCode;

    @Column(nullable = false)
    private String pageType;

    @Column(nullable = false)
    private String terminalType;

    @Column(nullable = false)
    private String pageName;

    @Column(nullable = false)
    private String pageStatus;

    private String currentSnapshotId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected DecorationPageEntity() {
    }

    public DecorationPageEntity(String pageId,
                                String storefrontId,
                                String pageCode,
                                String pageType,
                                String terminalType,
                                String pageName,
                                String pageStatus,
                                String currentSnapshotId,
                                OffsetDateTime createdAt,
                                OffsetDateTime updatedAt) {
        this.pageId = pageId;
        this.storefrontId = storefrontId;
        this.pageCode = pageCode;
        this.pageType = pageType;
        this.terminalType = terminalType;
        this.pageName = pageName;
        this.pageStatus = pageStatus;
        this.currentSnapshotId = currentSnapshotId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPageId() {
        return pageId;
    }

    public String getStorefrontId() {
        return storefrontId;
    }

    public String getPageCode() {
        return pageCode;
    }

    public String getPageType() {
        return pageType;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public String getPageName() {
        return pageName;
    }

    public String getPageStatus() {
        return pageStatus;
    }

    public String getCurrentSnapshotId() {
        return currentSnapshotId;
    }

    public void publish(String currentSnapshotId, OffsetDateTime updatedAt) {
        this.currentSnapshotId = currentSnapshotId;
        this.pageStatus = "PUBLISHED";
        this.updatedAt = updatedAt;
    }
}
