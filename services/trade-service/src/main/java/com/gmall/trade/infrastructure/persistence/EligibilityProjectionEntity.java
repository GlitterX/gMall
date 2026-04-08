package com.gmall.trade.infrastructure.persistence;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eligibility_projection")
public class EligibilityProjectionEntity {

    @Id
    private String projectionKey;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String storefrontId;

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false)
    private boolean eligible;

    @Column(nullable = false)
    private String organizationStatus;

    @Column(nullable = false)
    private String sellerStatus;

    @Column(nullable = false)
    private String storefrontStatus;

    @Column(nullable = false)
    private long organizationVersion;

    @Column(nullable = false)
    private long sellerVersion;

    @Column(nullable = false)
    private long storefrontVersion;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected EligibilityProjectionEntity() {
    }

    public EligibilityProjectionEntity(String sellerId, String storefrontId, String organizationId,
                                       boolean eligible, OffsetDateTime updatedAt) {
        this.projectionKey = sellerId + "::" + storefrontId;
        this.sellerId = sellerId;
        this.storefrontId = storefrontId;
        this.organizationId = organizationId;
        this.eligible = eligible;
        this.organizationStatus = "ACTIVE";
        this.sellerStatus = "ACTIVE";
        this.storefrontStatus = eligible ? "ACTIVE" : "FROZEN";
        this.organizationVersion = 0L;
        this.sellerVersion = 0L;
        this.storefrontVersion = 0L;
        this.updatedAt = updatedAt;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getStorefrontId() {
        return storefrontId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public boolean isEligible() {
        return eligible;
    }

    public long getStorefrontVersion() {
        return storefrontVersion;
    }

    public long getOrganizationVersion() {
        return organizationVersion;
    }

    public long getSellerVersion() {
        return sellerVersion;
    }

    public void applyOrganizationStatus(String organizationStatus, long version, OffsetDateTime updatedAt) {
        if (version <= organizationVersion) {
            return;
        }
        this.organizationStatus = organizationStatus;
        this.organizationVersion = version;
        recompute(updatedAt);
    }

    public void applySellerStatus(String sellerStatus, long version, OffsetDateTime updatedAt) {
        if (version <= sellerVersion) {
            return;
        }
        this.sellerStatus = sellerStatus;
        this.sellerVersion = version;
        recompute(updatedAt);
    }

    public void applyStorefrontStatus(boolean eligible, long version, OffsetDateTime updatedAt) {
        if (version <= storefrontVersion) {
            return;
        }
        this.storefrontStatus = eligible ? "ACTIVE" : "FROZEN";
        this.storefrontVersion = version;
        recompute(updatedAt);
    }

    private void recompute(OffsetDateTime updatedAt) {
        this.eligible = "ACTIVE".equals(organizationStatus)
                && "ACTIVE".equals(sellerStatus)
                && "ACTIVE".equals(storefrontStatus);
        this.updatedAt = updatedAt;
    }
}
