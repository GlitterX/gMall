package com.gmall.marketingcontent.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "marketing_projection_cache")
public class MarketingProjectionCacheEntity {

    @Id
    private String cacheId;

    @Column(nullable = false, length = 32)
    private String projectionType;

    @Column(nullable = false, length = 32)
    private String ownerType;

    @Column(nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, length = 16)
    private String terminalType;

    @Column(nullable = false, length = 64)
    private String pageContext;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(nullable = false, length = 16)
    private String resolvedLocale;

    @Column(nullable = false)
    private boolean fallbackApplied;

    @Column(nullable = false)
    private int cacheTtlSeconds;

    @Column(nullable = false, length = 16000)
    private String payload;

    @Column(nullable = false)
    private OffsetDateTime lastBuiltAt;

    protected MarketingProjectionCacheEntity() {
    }

    public MarketingProjectionCacheEntity(String cacheId,
                                          String projectionType,
                                          String ownerType,
                                          String ownerId,
                                          String terminalType,
                                          String pageContext,
                                          String locale,
                                          String resolvedLocale,
                                          boolean fallbackApplied,
                                          int cacheTtlSeconds,
                                          String payload,
                                          OffsetDateTime lastBuiltAt) {
        this.cacheId = cacheId;
        this.projectionType = projectionType;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.terminalType = terminalType;
        this.pageContext = pageContext;
        this.locale = locale;
        this.resolvedLocale = resolvedLocale;
        this.fallbackApplied = fallbackApplied;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.payload = payload;
        this.lastBuiltAt = lastBuiltAt;
    }

    public String getCacheId() {
        return cacheId;
    }

    public String getProjectionType() {
        return projectionType;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public String getPageContext() {
        return pageContext;
    }

    public String getLocale() {
        return locale;
    }

    public String getResolvedLocale() {
        return resolvedLocale;
    }

    public boolean isFallbackApplied() {
        return fallbackApplied;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public String getPayload() {
        return payload;
    }

    public OffsetDateTime getLastBuiltAt() {
        return lastBuiltAt;
    }

    public void refresh(String resolvedLocale,
                        boolean fallbackApplied,
                        int cacheTtlSeconds,
                        String payload,
                        OffsetDateTime lastBuiltAt) {
        this.resolvedLocale = resolvedLocale;
        this.fallbackApplied = fallbackApplied;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.payload = payload;
        this.lastBuiltAt = lastBuiltAt;
    }
}
