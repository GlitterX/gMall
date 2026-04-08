package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.OrganizationStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.messaging.FoundationEventPayload;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationOperatingProfileEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationOperatingProfileRepository;
import com.gmall.foundation.infrastructure.persistence.OrganizationRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class AdmissionApprovalProvisioner {

    private final OrganizationRepository organizationRepository;
    private final OrganizationOperatingProfileRepository operatingProfileRepository;
    private final FoundationEventAppender foundationEventAppender;

    public AdmissionApprovalProvisioner(OrganizationRepository organizationRepository,
                                        OrganizationOperatingProfileRepository operatingProfileRepository,
                                        FoundationEventAppender foundationEventAppender) {
        this.organizationRepository = organizationRepository;
        this.operatingProfileRepository = operatingProfileRepository;
        this.foundationEventAppender = foundationEventAppender;
    }

    public FoundationQueryModels.OrganizationView provision(String applicationId,
                                                            AdmissionApplicationEntity application,
                                                            AdmissionReviewCommand command) {
        OrganizationEntity organization = organizationRepository.save(
                new OrganizationEntity(
                        command.organizationId(),
                        application.getOrganizationType(),
                        application.getApplicantName(),
                        OrganizationStatus.PENDING,
                        applicationId,
                        command.defaultLocale(),
                        command.supportedLocales(),
                        command.reviewedBy(),
                        command.reviewComment(),
                        OffsetDateTime.now(),
                        1L
                )
        );
        operatingProfileRepository.save(
                new OrganizationOperatingProfileEntity(
                        organization.getOrganizationId(),
                        command.admissionChannel(),
                        command.industryCategory(),
                        command.businessScope(),
                        command.remark(),
                        OffsetDateTime.now()
                )
        );
        foundationEventAppender.append(
                "Organization",
                organization.getOrganizationId(),
                "OrganizationAdmissionApproved",
                organization.getAggregateVersion(),
                new FoundationEventPayload(
                        organization.getOrganizationId(),
                        null,
                        null,
                        organization.getStatus().name(),
                        command.reviewedBy(),
                        command.reviewComment()
                )
        );
        return new FoundationQueryModels.OrganizationView(
                organization.getOrganizationId(),
                organization.getOrganizationType(),
                organization.getOrganizationName(),
                organization.getStatus().name(),
                organization.getDefaultLocale(),
                organization.getSupportedLocales(),
                organization.getSourceApplicationId(),
                organization.getAggregateVersion()
        );
    }
}
