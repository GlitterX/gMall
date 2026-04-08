package com.gmall.foundation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "organization_operating_profile")
public class OrganizationOperatingProfileEntity {

    @Id
    private String organizationId;

    @Column(nullable = false)
    private String admissionChannel;

    @Column(nullable = false)
    private String industryCategory;

    @Column(nullable = false)
    private String businessScope;

    @Column(nullable = false)
    private String remark;

    @Column(nullable = false)
    private OffsetDateTime lastVerifiedAt;

    protected OrganizationOperatingProfileEntity() {
    }

    public OrganizationOperatingProfileEntity(String organizationId,
                                              String admissionChannel,
                                              String industryCategory,
                                              String businessScope,
                                              String remark,
                                              OffsetDateTime lastVerifiedAt) {
        this.organizationId = organizationId;
        this.admissionChannel = admissionChannel;
        this.industryCategory = industryCategory;
        this.businessScope = businessScope;
        this.remark = remark;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getAdmissionChannel() {
        return admissionChannel;
    }

    public String getIndustryCategory() {
        return industryCategory;
    }
}
