package com.gmall.foundation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "storefront_terminal")
public class StorefrontTerminalEntity {

    @Id
    private String terminalKey;

    @Column(nullable = false)
    private String storefrontId;

    @Column(nullable = false)
    private String terminalType;

    @Column(nullable = false)
    private boolean enabled;

    private String homePageId;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false)
    private String operationReason;

    @Column(nullable = false)
    private OffsetDateTime operationAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    protected StorefrontTerminalEntity() {
    }

    public StorefrontTerminalEntity(String storefrontId,
                                    String terminalType,
                                    boolean enabled,
                                    String homePageId,
                                    String operatorId,
                                    String operationReason,
                                    OffsetDateTime operationAt,
                                    Long aggregateVersion) {
        this.terminalKey = buildKey(storefrontId, terminalType);
        this.storefrontId = storefrontId;
        this.terminalType = terminalType;
        this.enabled = enabled;
        this.homePageId = homePageId;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion;
    }

    public static String buildKey(String storefrontId, String terminalType) {
        return storefrontId + "::" + terminalType;
    }

    public String getTerminalKey() {
        return terminalKey;
    }

    public String getStorefrontId() {
        return storefrontId;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getHomePageId() {
        return homePageId;
    }

    public Long getAggregateVersion() {
        return aggregateVersion;
    }

    public void update(boolean enabled,
                       String homePageId,
                       String operatorId,
                       String operationReason,
                       OffsetDateTime operationAt) {
        this.enabled = enabled;
        this.homePageId = homePageId;
        this.operatorId = operatorId;
        this.operationReason = operationReason;
        this.operationAt = operationAt;
        this.aggregateVersion = aggregateVersion + 1;
    }
}
