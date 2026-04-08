package com.gmall.foundation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmall.foundation.domain.model.AdmissionApplicationStatus;
import com.gmall.foundation.domain.model.AdmissionReviewTaskStatus;
import com.gmall.foundation.domain.model.OrganizationStatus;
import com.gmall.foundation.infrastructure.messaging.FoundationEventAppender;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationRepository;
import com.gmall.foundation.infrastructure.persistence.AdmissionOperationLogEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionOperationLogRepository;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewRecordEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewRecordRepository;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewTaskEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewTaskRepository;
import com.gmall.foundation.infrastructure.persistence.OrganizationEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationOperatingProfileEntity;
import com.gmall.foundation.infrastructure.persistence.OrganizationOperatingProfileRepository;
import com.gmall.foundation.infrastructure.persistence.OrganizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FoundationAdmissionServiceTest {

    private final AdmissionApplicationRepository admissionApplicationRepository = mock(AdmissionApplicationRepository.class);
    private final AdmissionOperationLogRepository admissionOperationLogRepository = mock(AdmissionOperationLogRepository.class);
    private final AdmissionReviewRecordRepository admissionReviewRecordRepository = mock(AdmissionReviewRecordRepository.class);
    private final AdmissionReviewTaskRepository admissionReviewTaskRepository = mock(AdmissionReviewTaskRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final OrganizationOperatingProfileRepository operatingProfileRepository = mock(OrganizationOperatingProfileRepository.class);
    private final FoundationEventAppender foundationEventAppender = mock(FoundationEventAppender.class);

    private final List<AdmissionApplicationEntity> savedApplications = new ArrayList<>();
    private final List<AdmissionOperationLogEntity> savedOperationLogs = new ArrayList<>();
    private final List<AdmissionReviewRecordEntity> savedReviewRecords = new ArrayList<>();
    private final List<AdmissionReviewTaskEntity> savedReviewTasks = new ArrayList<>();
    private final List<OrganizationEntity> savedOrganizations = new ArrayList<>();
    private final List<OrganizationOperatingProfileEntity> savedProfiles = new ArrayList<>();

    private FoundationAdmissionService foundationAdmissionService;
    private AdmissionIdempotencySupport admissionIdempotencySupport;
    private AdmissionApprovalProvisioner admissionApprovalProvisioner;
    private AdmissionWorkflowSupport admissionWorkflowSupport;
    private AdmissionReviewTaskSupport admissionReviewTaskSupport;

    @BeforeEach
    void setUp() {
        admissionIdempotencySupport = new AdmissionIdempotencySupport(
                admissionApplicationRepository,
                admissionOperationLogRepository,
                organizationRepository
        );
        admissionApprovalProvisioner = new AdmissionApprovalProvisioner(
                organizationRepository,
                operatingProfileRepository,
                foundationEventAppender
        );
        admissionWorkflowSupport = new AdmissionWorkflowSupport(
                admissionReviewRecordRepository,
                foundationEventAppender
        );
        admissionReviewTaskSupport = new AdmissionReviewTaskSupport(
                admissionReviewTaskRepository,
                admissionApplicationRepository,
                Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.ofHours(8))
        );
        foundationAdmissionService = new FoundationAdmissionService(
                admissionApplicationRepository,
                admissionIdempotencySupport,
                admissionApprovalProvisioner,
                admissionWorkflowSupport,
                admissionReviewTaskSupport
        );
        when(admissionApplicationRepository.save(any(AdmissionApplicationEntity.class)))
                .thenAnswer(invocation -> {
                    AdmissionApplicationEntity entity = invocation.getArgument(0);
                    savedApplications.add(entity);
                    return entity;
                });
        when(admissionOperationLogRepository.save(any(AdmissionOperationLogEntity.class)))
                .thenAnswer(invocation -> {
                    AdmissionOperationLogEntity entity = invocation.getArgument(0);
                    savedOperationLogs.add(entity);
                    return entity;
                });
        when(admissionReviewRecordRepository.save(any(AdmissionReviewRecordEntity.class)))
                .thenAnswer(invocation -> {
                    AdmissionReviewRecordEntity entity = invocation.getArgument(0);
                    savedReviewRecords.add(entity);
                    return entity;
                });
        when(admissionReviewTaskRepository.save(any(AdmissionReviewTaskEntity.class)))
                .thenAnswer(invocation -> {
                    AdmissionReviewTaskEntity entity = invocation.getArgument(0);
                    savedReviewTasks.add(entity);
                    return entity;
                });
        when(organizationRepository.save(any(OrganizationEntity.class)))
                .thenAnswer(invocation -> {
                    OrganizationEntity entity = invocation.getArgument(0);
                    savedOrganizations.add(entity);
                    return entity;
                });
        when(operatingProfileRepository.save(any(OrganizationOperatingProfileEntity.class)))
                .thenAnswer(invocation -> {
                    OrganizationOperatingProfileEntity entity = invocation.getArgument(0);
                    savedProfiles.add(entity);
                    return entity;
                });
    }

    @Test
    void startReviewMarksApplicationUnderReview() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-0",
                "MERCHANT",
                AdmissionApplicationStatus.SUBMITTED,
                "测试商家",
                "LICENSE-0",
                "审核员",
                "13800138001",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-03-31T09:00:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-0")).thenReturn(Optional.of(application));

        AdmissionApplicationEntity reviewing = foundationAdmissionService.startReview(
                "app-0",
                new FoundationOperationCommand("reviewer", "开始审核")
        );

        assertThat(reviewing.getApplicationStatus()).isEqualTo(AdmissionApplicationStatus.UNDER_REVIEW);
        assertThat(reviewing.getReviewComment()).isEqualTo("开始审核");
        assertThat(reviewing.getReviewedBy()).isEqualTo("reviewer");
        assertThat(reviewing.getAggregateVersion()).isEqualTo(2L);
        assertThat(savedReviewRecords).singleElement().satisfies(record -> {
            assertThat(record.getApplicationId()).isEqualTo("app-0");
            assertThat(record.getFromStatus()).isEqualTo("SUBMITTED");
            assertThat(record.getToStatus()).isEqualTo("UNDER_REVIEW");
            assertThat(record.getActionType()).isEqualTo("START_REVIEW");
            assertThat(record.getOperatorId()).isEqualTo("reviewer");
            assertThat(record.getOperationReason()).isEqualTo("开始审核");
            assertThat(record.getAggregateVersion()).isEqualTo(2L);
        });
        assertThat(savedOperationLogs).singleElement().satisfies(log -> {
            assertThat(log.getApplicationId()).isEqualTo("app-0");
            assertThat(log.getActionType()).isEqualTo("START_REVIEW");
            assertThat(log.getAggregateVersion()).isEqualTo(2L);
        });
        assertThat(savedReviewTasks).singleElement().satisfies(task -> {
            assertThat(task.getApplicationId()).isEqualTo("app-0");
            assertThat(task.getTaskSequence()).isEqualTo(1L);
            assertThat(task.getTaskStatus()).isEqualTo(AdmissionReviewTaskStatus.ACTIVE);
            assertThat(task.getReviewerId()).isEqualTo("reviewer");
        });
    }

    @Test
    void submitReturnsExistingApplicationWhenSameApplicationIdAndPayloadAreRetried() {
        AdmissionApplicationEntity existing = new AdmissionApplicationEntity(
                "app-0-repeat",
                "MERCHANT",
                AdmissionApplicationStatus.SUBMITTED,
                "测试商家",
                "LICENSE-0-REPEAT",
                "审核员",
                "13800138009",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T08:50:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-0-repeat")).thenReturn(Optional.of(existing));

        AdmissionApplicationEntity actual = foundationAdmissionService.submit(
                new AdmissionApplicationCommand(
                        "app-0-repeat",
                        "MERCHANT",
                        "测试商家",
                        "LICENSE-0-REPEAT",
                        "审核员",
                        "13800138009"
                )
        );

        assertThat(actual).isSameAs(existing);
        assertThat(savedApplications).isEmpty();
        assertThat(savedOperationLogs).isEmpty();
    }

    @Test
    void submitRejectsConflictingDuplicateApplicationId() {
        AdmissionApplicationEntity existing = new AdmissionApplicationEntity(
                "app-0-conflict",
                "MERCHANT",
                AdmissionApplicationStatus.SUBMITTED,
                "测试商家",
                "LICENSE-0-CONFLICT",
                "审核员",
                "13800138008",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T08:52:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-0-conflict")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> foundationAdmissionService.submit(
                new AdmissionApplicationCommand(
                        "app-0-conflict",
                        "MERCHANT",
                        "测试商家新名称",
                        "LICENSE-0-CONFLICT",
                        "审核员",
                        "13800138008"
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("申请编号已存在且载荷不一致: app-0-conflict");
    }

    @Test
    void approveReviewCreatesPendingOrganizationAndOperatingProfile() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-1",
                "MERCHANT",
                AdmissionApplicationStatus.UNDER_REVIEW,
                "测试商家",
                "LICENSE-1",
                "张三",
                "13800138000",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-03-31T10:00:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-1")).thenReturn(Optional.of(application));
        when(admissionReviewTaskRepository.findFirstByApplicationIdAndTaskStatusOrderByTaskSequenceDesc(
                "app-1",
                AdmissionReviewTaskStatus.ACTIVE
        )).thenReturn(Optional.of(new AdmissionReviewTaskEntity(
                "app-1",
                1L,
                AdmissionReviewTaskStatus.ACTIVE,
                "reviewer",
                OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                OffsetDateTime.parse("2026-04-02T09:00:00+08:00"),
                "reviewer",
                "开始审核",
                OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                null,
                1L
        )));

        FoundationQueryModels.OrganizationView organizationView = foundationAdmissionService.review(
                "app-1",
                new AdmissionReviewCommand(
                        "APPROVED",
                        "资料齐全",
                        "reviewer",
                        "org-1",
                        "zh-CN",
                        "zh-CN,en-US",
                        "平台招商",
                        "FOOD",
                        "零售",
                        "通过"
                )
        );

        assertThat(organizationView.organizationId()).isEqualTo("org-1");
        assertThat(organizationView.organizationName()).isEqualTo("测试商家");
        assertThat(organizationView.status()).isEqualTo("PENDING");
        assertThat(savedApplications).singleElement().satisfies(entity -> {
            assertThat(entity.getApplicationStatus()).isEqualTo(AdmissionApplicationStatus.APPROVED);
            assertThat(entity.getReviewComment()).isEqualTo("资料齐全");
            assertThat(entity.getReviewedBy()).isEqualTo("reviewer");
        });
        assertThat(savedOrganizations).singleElement().satisfies(entity -> {
            assertThat(entity.getOrganizationId()).isEqualTo("org-1");
            assertThat(entity.getOrganizationType()).isEqualTo("MERCHANT");
            assertThat(entity.getOrganizationName()).isEqualTo("测试商家");
            assertThat(entity.getStatus()).isEqualTo(OrganizationStatus.PENDING);
            assertThat(entity.getSourceApplicationId()).isEqualTo("app-1");
        });
        assertThat(savedProfiles).singleElement().satisfies(entity -> {
            assertThat(entity.getOrganizationId()).isEqualTo("org-1");
            assertThat(entity.getAdmissionChannel()).isEqualTo("平台招商");
            assertThat(entity.getIndustryCategory()).isEqualTo("FOOD");
        });
        assertThat(savedReviewRecords).singleElement().satisfies(record -> {
            assertThat(record.getApplicationId()).isEqualTo("app-1");
            assertThat(record.getFromStatus()).isEqualTo("UNDER_REVIEW");
            assertThat(record.getToStatus()).isEqualTo("APPROVED");
            assertThat(record.getActionType()).isEqualTo("APPROVE");
            assertThat(record.getAggregateVersion()).isEqualTo(2L);
        });
        assertThat(savedOperationLogs).singleElement().satisfies(log -> {
            assertThat(log.getApplicationId()).isEqualTo("app-1");
            assertThat(log.getActionType()).isEqualTo("APPROVE");
            assertThat(log.getAggregateVersion()).isEqualTo(2L);
        });
        assertThat(savedReviewTasks).singleElement().satisfies(task -> {
            assertThat(task.getApplicationId()).isEqualTo("app-1");
            assertThat(task.getTaskStatus()).isEqualTo(AdmissionReviewTaskStatus.APPROVED);
            assertThat(task.getReviewerId()).isEqualTo("reviewer");
            assertThat(task.getClosedAt()).isNotNull();
        });
        verify(foundationEventAppender).append(
                org.mockito.ArgumentMatchers.eq("Organization"),
                org.mockito.ArgumentMatchers.eq("org-1"),
                org.mockito.ArgumentMatchers.eq("OrganizationAdmissionApproved"),
                org.mockito.ArgumentMatchers.eq(1L),
                any()
        );
    }

    @Test
    void approveReviewReturnsExistingOrganizationWhenOperationIsRetried() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-1-approved",
                "MERCHANT",
                AdmissionApplicationStatus.APPROVED,
                "测试商家",
                "LICENSE-1-APPROVED",
                "张三",
                "13800138006",
                "资料齐全",
                "reviewer",
                OffsetDateTime.parse("2026-04-01T09:20:00+08:00"),
                OffsetDateTime.parse("2026-04-01T08:40:00+08:00"),
                2L
        );
        OrganizationEntity organization = new OrganizationEntity(
                "org-1-approved",
                "MERCHANT",
                "测试商家",
                OrganizationStatus.PENDING,
                "app-1-approved",
                "zh-CN",
                "zh-CN,en-US",
                "reviewer",
                "资料齐全",
                OffsetDateTime.parse("2026-04-01T09:20:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-1-approved")).thenReturn(Optional.of(application));
        when(organizationRepository.findById("org-1-approved")).thenReturn(Optional.of(organization));

        FoundationQueryModels.OrganizationView actual = foundationAdmissionService.review(
                "app-1-approved",
                new AdmissionReviewCommand(
                        "APPROVED",
                        "资料齐全",
                        "reviewer",
                        "org-1-approved",
                        "zh-CN",
                        "zh-CN,en-US",
                        "平台招商",
                        "FOOD",
                        "零售",
                        "通过"
                )
        );

        assertThat(actual.organizationId()).isEqualTo("org-1-approved");
        assertThat(actual.status()).isEqualTo("PENDING");
        assertThat(savedOrganizations).isEmpty();
        assertThat(savedProfiles).isEmpty();
        assertThat(savedReviewRecords).isEmpty();
        assertThat(savedOperationLogs).isEmpty();
    }

    @Test
    void startReviewReturnsExistingApplicationWhenOperationIsRetried() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-1-repeat",
                "MERCHANT",
                AdmissionApplicationStatus.UNDER_REVIEW,
                "测试商家",
                "LICENSE-1-REPEAT",
                "张三",
                "13800138007",
                "开始审核",
                "reviewer",
                OffsetDateTime.parse("2026-04-01T09:10:00+08:00"),
                OffsetDateTime.parse("2026-04-01T08:30:00+08:00"),
                2L
        );
        when(admissionApplicationRepository.findById("app-1-repeat")).thenReturn(Optional.of(application));

        AdmissionApplicationEntity actual = foundationAdmissionService.startReview(
                "app-1-repeat",
                new FoundationOperationCommand("reviewer", "开始审核")
        );

        assertThat(actual).isSameAs(application);
        assertThat(savedReviewRecords).isEmpty();
        assertThat(savedOperationLogs).isEmpty();
    }

    @Test
    void rejectMarksApplicationRejectedWithoutCreatingOrganization() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-2",
                "SUPPLIER",
                AdmissionApplicationStatus.UNDER_REVIEW,
                "测试供应商",
                "LICENSE-2",
                "李四",
                "13900139000",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-2")).thenReturn(Optional.of(application));
        when(admissionReviewTaskRepository.findFirstByApplicationIdAndTaskStatusOrderByTaskSequenceDesc(
                "app-2",
                AdmissionReviewTaskStatus.ACTIVE
        )).thenReturn(Optional.of(new AdmissionReviewTaskEntity(
                "app-2",
                1L,
                AdmissionReviewTaskStatus.ACTIVE,
                "reviewer",
                OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                OffsetDateTime.parse("2026-04-02T09:00:00+08:00"),
                "reviewer",
                "开始审核",
                OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                null,
                1L
        )));

        AdmissionApplicationEntity rejected = foundationAdmissionService.reject(
                "app-2",
                new FoundationOperationCommand("reviewer", "资料不完整")
        );

        assertThat(rejected.getApplicationStatus()).isEqualTo(AdmissionApplicationStatus.REJECTED);
        assertThat(rejected.getReviewComment()).isEqualTo("资料不完整");
        assertThat(rejected.getReviewedBy()).isEqualTo("reviewer");
        assertThat(savedOrganizations).isEmpty();
        assertThat(savedProfiles).isEmpty();
        assertThat(savedReviewRecords).singleElement().satisfies(record -> {
            assertThat(record.getApplicationId()).isEqualTo("app-2");
            assertThat(record.getFromStatus()).isEqualTo("UNDER_REVIEW");
            assertThat(record.getToStatus()).isEqualTo("REJECTED");
            assertThat(record.getActionType()).isEqualTo("REJECT");
            assertThat(record.getAggregateVersion()).isEqualTo(2L);
        });
        assertThat(savedOperationLogs).singleElement().satisfies(log -> {
            assertThat(log.getApplicationId()).isEqualTo("app-2");
            assertThat(log.getActionType()).isEqualTo("REJECT");
            assertThat(log.getAggregateVersion()).isEqualTo(2L);
        });
        assertThat(savedReviewTasks).singleElement().satisfies(task -> {
            assertThat(task.getApplicationId()).isEqualTo("app-2");
            assertThat(task.getTaskStatus()).isEqualTo(AdmissionReviewTaskStatus.REJECTED);
            assertThat(task.getClosedAt()).isNotNull();
        });
        verify(foundationEventAppender).append(
                org.mockito.ArgumentMatchers.eq("AdmissionApplication"),
                org.mockito.ArgumentMatchers.eq("app-2"),
                org.mockito.ArgumentMatchers.eq("OrganizationAdmissionRejected"),
                org.mockito.ArgumentMatchers.eq(2L),
                any()
        );
    }

    @Test
    void rejectRequiresUnderReviewStatus() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-2b",
                "SUPPLIER",
                AdmissionApplicationStatus.SUBMITTED,
                "测试供应商",
                "LICENSE-2B",
                "李四",
                "13900139001",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T09:05:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-2b")).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> foundationAdmissionService.reject(
                "app-2b",
                new FoundationOperationCommand("reviewer", "资料不完整")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("当前状态不允许审核拒绝: SUBMITTED");
    }

    @Test
    void withdrawMarksSubmittedApplicationWithdrawn() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-3",
                "MERCHANT",
                AdmissionApplicationStatus.SUBMITTED,
                "测试商家",
                "LICENSE-3",
                "王五",
                "13700137000",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T09:30:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-3")).thenReturn(Optional.of(application));

        AdmissionApplicationEntity withdrawn = foundationAdmissionService.withdraw(
                "app-3",
                new FoundationOperationCommand("applicant", "主动撤回")
        );

        assertThat(withdrawn.getApplicationStatus()).isEqualTo(AdmissionApplicationStatus.WITHDRAWN);
        assertThat(withdrawn.getReviewComment()).isEqualTo("主动撤回");
        assertThat(withdrawn.getReviewedBy()).isEqualTo("applicant");
        assertThat(withdrawn.getAggregateVersion()).isEqualTo(2L);
        assertThat(savedReviewRecords).singleElement().satisfies(record -> {
            assertThat(record.getApplicationId()).isEqualTo("app-3");
            assertThat(record.getFromStatus()).isEqualTo("SUBMITTED");
            assertThat(record.getToStatus()).isEqualTo("WITHDRAWN");
            assertThat(record.getActionType()).isEqualTo("WITHDRAW");
            assertThat(record.getAggregateVersion()).isEqualTo(2L);
        });
        assertThat(savedOperationLogs).singleElement().satisfies(log -> {
            assertThat(log.getApplicationId()).isEqualTo("app-3");
            assertThat(log.getActionType()).isEqualTo("WITHDRAW");
            assertThat(log.getAggregateVersion()).isEqualTo(2L);
        });
    }

    @Test
    void resubmitReturnsExistingApplicationWhenSamePayloadIsRetried() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-4-repeat",
                "MERCHANT",
                AdmissionApplicationStatus.SUBMITTED,
                "新商家名称",
                "LICENSE-4-REPEAT",
                "赵六",
                "13600136009",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T09:15:00+08:00"),
                3L
        );
        when(admissionApplicationRepository.findById("app-4-repeat")).thenReturn(Optional.of(application));

        AdmissionApplicationEntity actual = foundationAdmissionService.resubmit(
                "app-4-repeat",
                new AdmissionResubmitCommand(
                        "app-4-repeat",
                        "MERCHANT",
                        "新商家名称",
                        "LICENSE-4-REPEAT",
                        "赵六",
                        "13600136009",
                        "merchant-operator",
                        "补充资料后重新提交"
                )
        );

        assertThat(actual).isSameAs(application);
        assertThat(savedReviewRecords).isEmpty();
        assertThat(savedOperationLogs).isEmpty();
    }

    @Test
    void resubmitAllowsRejectedApplicationToReturnToSubmitted() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-4",
                "MERCHANT",
                AdmissionApplicationStatus.REJECTED,
                "旧商家名称",
                "LICENSE-OLD",
                "赵六",
                "13600136000",
                "资料不完整",
                "reviewer",
                OffsetDateTime.parse("2026-04-01T08:00:00+08:00"),
                OffsetDateTime.parse("2026-04-01T07:30:00+08:00"),
                2L
        );
        when(admissionApplicationRepository.findById("app-4")).thenReturn(Optional.of(application));

        AdmissionApplicationEntity resubmitted = foundationAdmissionService.resubmit(
                "app-4",
                new AdmissionResubmitCommand(
                        "app-4",
                        "MERCHANT",
                        "新商家名称",
                        "LICENSE-NEW",
                        "赵六",
                        "13600136001",
                        "merchant-operator",
                        "补充资料后重新提交"
                )
        );

        assertThat(resubmitted.getApplicationStatus()).isEqualTo(AdmissionApplicationStatus.SUBMITTED);
        assertThat(resubmitted.getApplicantName()).isEqualTo("新商家名称");
        assertThat(resubmitted.getBusinessLicenseNo()).isEqualTo("LICENSE-NEW");
        assertThat(resubmitted.getContactMobile()).isEqualTo("13600136001");
        assertThat(resubmitted.getReviewComment()).isNull();
        assertThat(resubmitted.getReviewedBy()).isNull();
        assertThat(resubmitted.getReviewedAt()).isNull();
        assertThat(resubmitted.getAggregateVersion()).isEqualTo(3L);
        assertThat(savedReviewRecords).singleElement().satisfies(record -> {
            assertThat(record.getApplicationId()).isEqualTo("app-4");
            assertThat(record.getFromStatus()).isEqualTo("REJECTED");
            assertThat(record.getToStatus()).isEqualTo("SUBMITTED");
            assertThat(record.getActionType()).isEqualTo("RESUBMIT");
            assertThat(record.getOperatorId()).isEqualTo("merchant-operator");
            assertThat(record.getOperationReason()).isEqualTo("补充资料后重新提交");
            assertThat(record.getAggregateVersion()).isEqualTo(3L);
        });
        assertThat(savedOperationLogs).singleElement().satisfies(log -> {
            assertThat(log.getApplicationId()).isEqualTo("app-4");
            assertThat(log.getActionType()).isEqualTo("RESUBMIT");
            assertThat(log.getAggregateVersion()).isEqualTo(3L);
        });
    }

    @Test
    void resubmitRequiresRejectedStatus() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-5",
                "MERCHANT",
                AdmissionApplicationStatus.SUBMITTED,
                "测试商家",
                "LICENSE-5",
                "孙七",
                "13500135000",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T09:45:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-5")).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> foundationAdmissionService.resubmit(
                "app-5",
                new AdmissionResubmitCommand(
                        "app-5",
                        "MERCHANT",
                        "测试商家",
                        "LICENSE-5",
                        "孙七",
                        "13500135000",
                        "merchant-operator",
                        "再次提交"
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("当前状态不允许重提: SUBMITTED");
    }

    @Test
    void reassignReviewChangesReviewerWithinUnderReview() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-6",
                "MERCHANT",
                AdmissionApplicationStatus.UNDER_REVIEW,
                "测试商家",
                "LICENSE-6",
                "周八",
                "13400134000",
                "开始审核",
                "reviewer-a",
                OffsetDateTime.parse("2026-04-01T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-01T09:30:00+08:00"),
                2L
        );
        when(admissionApplicationRepository.findById("app-6")).thenReturn(Optional.of(application));
        when(admissionReviewTaskRepository.findFirstByApplicationIdAndTaskStatusOrderByTaskSequenceDesc(
                "app-6",
                AdmissionReviewTaskStatus.ACTIVE
        )).thenReturn(Optional.of(new AdmissionReviewTaskEntity(
                "app-6",
                1L,
                AdmissionReviewTaskStatus.ACTIVE,
                "reviewer-a",
                OffsetDateTime.parse("2026-04-01T10:00:00+08:00"),
                OffsetDateTime.parse("2026-04-02T10:00:00+08:00"),
                "reviewer-a",
                "开始审核",
                OffsetDateTime.parse("2026-04-01T10:00:00+08:00"),
                null,
                1L
        )));

        AdmissionApplicationEntity reassigned = foundationAdmissionService.reassignReview(
                "app-6",
                new AdmissionReviewAssignmentCommand("ops-lead", "reviewer-b", "转派给复审")
        );

        assertThat(reassigned.getApplicationStatus()).isEqualTo(AdmissionApplicationStatus.UNDER_REVIEW);
        assertThat(reassigned.getReviewedBy()).isEqualTo("reviewer-b");
        assertThat(reassigned.getReviewComment()).isEqualTo("转派给复审");
        assertThat(reassigned.getAggregateVersion()).isEqualTo(3L);
        assertThat(savedReviewRecords).singleElement().satisfies(record -> {
            assertThat(record.getApplicationId()).isEqualTo("app-6");
            assertThat(record.getFromStatus()).isEqualTo("UNDER_REVIEW");
            assertThat(record.getToStatus()).isEqualTo("UNDER_REVIEW");
            assertThat(record.getActionType()).isEqualTo("REASSIGN_REVIEW");
            assertThat(record.getOperatorId()).isEqualTo("ops-lead");
            assertThat(record.getOperationReason()).contains("reviewer-b");
            assertThat(record.getAggregateVersion()).isEqualTo(3L);
        });
        assertThat(savedOperationLogs).singleElement().satisfies(log -> {
            assertThat(log.getApplicationId()).isEqualTo("app-6");
            assertThat(log.getActionType()).isEqualTo("REASSIGN_REVIEW");
            assertThat(log.getAggregateVersion()).isEqualTo(3L);
        });
        assertThat(savedReviewTasks).singleElement().satisfies(task -> {
            assertThat(task.getApplicationId()).isEqualTo("app-6");
            assertThat(task.getTaskStatus()).isEqualTo(AdmissionReviewTaskStatus.ACTIVE);
            assertThat(task.getReviewerId()).isEqualTo("reviewer-b");
        });
    }

    @Test
    void reassignReviewReturnsExistingApplicationWhenOperationIsRetried() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-6-repeat",
                "MERCHANT",
                AdmissionApplicationStatus.UNDER_REVIEW,
                "测试商家",
                "LICENSE-6-REPEAT",
                "周八",
                "13400134001",
                "转派给复审",
                "reviewer-b",
                OffsetDateTime.parse("2026-04-01T10:20:00+08:00"),
                OffsetDateTime.parse("2026-04-01T09:30:00+08:00"),
                3L
        );
        when(admissionApplicationRepository.findById("app-6-repeat")).thenReturn(Optional.of(application));

        AdmissionApplicationEntity actual = foundationAdmissionService.reassignReview(
                "app-6-repeat",
                new AdmissionReviewAssignmentCommand("ops-lead", "reviewer-b", "转派给复审")
        );

        assertThat(actual).isSameAs(application);
        assertThat(savedReviewRecords).isEmpty();
        assertThat(savedOperationLogs).isEmpty();
    }

    @Test
    void reassignReviewRequiresUnderReviewStatus() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-7",
                "MERCHANT",
                AdmissionApplicationStatus.SUBMITTED,
                "测试商家",
                "LICENSE-7",
                "周九",
                "13400134002",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T09:50:00+08:00"),
                1L
        );
        when(admissionApplicationRepository.findById("app-7")).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> foundationAdmissionService.reassignReview(
                "app-7",
                new AdmissionReviewAssignmentCommand("ops-lead", "reviewer-b", "转派给复审")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("当前状态不允许转派审核: SUBMITTED");
    }

    @Test
    void timeoutReviewReturnsApplicationToSubmittedWhenReviewExceededSla() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-8",
                "MERCHANT",
                AdmissionApplicationStatus.UNDER_REVIEW,
                "测试商家",
                "LICENSE-8",
                "钱十",
                "13300133000",
                "转派给复审",
                "reviewer-c",
                OffsetDateTime.now().minusHours(30),
                OffsetDateTime.parse("2026-04-01T09:00:00+08:00"),
                3L
        );
        when(admissionApplicationRepository.findById("app-8")).thenReturn(Optional.of(application));
        when(admissionReviewTaskRepository.findFirstByApplicationIdAndTaskStatusOrderByTaskSequenceDesc(
                "app-8",
                AdmissionReviewTaskStatus.ACTIVE
        )).thenReturn(Optional.of(new AdmissionReviewTaskEntity(
                "app-8",
                1L,
                AdmissionReviewTaskStatus.ACTIVE,
                "reviewer-c",
                OffsetDateTime.now().minusHours(30),
                OffsetDateTime.now().minusHours(6),
                "ops-lead",
                "转派给复审",
                OffsetDateTime.now().minusHours(30),
                null,
                2L
        )));

        AdmissionApplicationEntity timedOut = foundationAdmissionService.timeoutReview(
                "app-8",
                new FoundationOperationCommand("ops-supervisor", "审核超时退回待分配")
        );

        assertThat(timedOut.getApplicationStatus()).isEqualTo(AdmissionApplicationStatus.SUBMITTED);
        assertThat(timedOut.getReviewedBy()).isEqualTo("ops-supervisor");
        assertThat(timedOut.getReviewComment()).isEqualTo("审核超时退回待分配");
        assertThat(timedOut.getAggregateVersion()).isEqualTo(4L);
        assertThat(savedReviewRecords).singleElement().satisfies(record -> {
            assertThat(record.getApplicationId()).isEqualTo("app-8");
            assertThat(record.getFromStatus()).isEqualTo("UNDER_REVIEW");
            assertThat(record.getToStatus()).isEqualTo("SUBMITTED");
            assertThat(record.getActionType()).isEqualTo("TIMEOUT_REVIEW");
            assertThat(record.getOperatorId()).isEqualTo("ops-supervisor");
            assertThat(record.getOperationReason()).isEqualTo("审核超时退回待分配");
            assertThat(record.getAggregateVersion()).isEqualTo(4L);
        });
        assertThat(savedOperationLogs).singleElement().satisfies(log -> {
            assertThat(log.getApplicationId()).isEqualTo("app-8");
            assertThat(log.getActionType()).isEqualTo("TIMEOUT_REVIEW");
            assertThat(log.getAggregateVersion()).isEqualTo(4L);
        });
        assertThat(savedReviewTasks).singleElement().satisfies(task -> {
            assertThat(task.getApplicationId()).isEqualTo("app-8");
            assertThat(task.getTaskStatus()).isEqualTo(AdmissionReviewTaskStatus.TIMED_OUT);
            assertThat(task.getReviewerId()).isEqualTo("reviewer-c");
            assertThat(task.getClosedAt()).isNotNull();
        });
    }

    @Test
    void timeoutReviewRejectsApplicationThatHasNotExceededSla() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-9",
                "MERCHANT",
                AdmissionApplicationStatus.UNDER_REVIEW,
                "测试商家",
                "LICENSE-9",
                "吴十一",
                "13200132000",
                "开始审核",
                "reviewer-d",
                OffsetDateTime.now().minusHours(2),
                OffsetDateTime.parse("2026-04-01T09:20:00+08:00"),
                2L
        );
        when(admissionApplicationRepository.findById("app-9")).thenReturn(Optional.of(application));
        when(admissionReviewTaskRepository.findFirstByApplicationIdAndTaskStatusOrderByTaskSequenceDesc(
                "app-9",
                AdmissionReviewTaskStatus.ACTIVE
        )).thenReturn(Optional.of(new AdmissionReviewTaskEntity(
                "app-9",
                1L,
                AdmissionReviewTaskStatus.ACTIVE,
                "reviewer-d",
                OffsetDateTime.now().minusHours(2),
                OffsetDateTime.now().plusHours(22),
                "reviewer-d",
                "开始审核",
                OffsetDateTime.now().minusHours(2),
                null,
                1L
        )));

        assertThatThrownBy(() -> foundationAdmissionService.timeoutReview(
                "app-9",
                new FoundationOperationCommand("ops-supervisor", "审核超时退回待分配")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("当前审核未超时，不能退回待分配");
    }

    @Test
    void getReviewTaskReturnsLatestTaskReadModel() {
        when(admissionReviewTaskRepository.findFirstByApplicationIdOrderByTaskSequenceDesc("app-11"))
                .thenReturn(Optional.of(new AdmissionReviewTaskEntity(
                        "app-11",
                        2L,
                        AdmissionReviewTaskStatus.ACTIVE,
                        "reviewer-b",
                        OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                        OffsetDateTime.parse("2026-04-02T10:30:00+08:00"),
                        "ops-lead",
                        "转派给复审",
                        OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                        null,
                        3L
                )));

        FoundationQueryModels.AdmissionReviewTaskView actual = foundationAdmissionService.getReviewTask("app-11");

        assertThat(actual.applicationId()).isEqualTo("app-11");
        assertThat(actual.taskSequence()).isEqualTo(2L);
        assertThat(actual.taskStatus()).isEqualTo("ACTIVE");
        assertThat(actual.reviewerId()).isEqualTo("reviewer-b");
        assertThat(actual.lastOperatedBy()).isEqualTo("ops-lead");
    }

    @Test
    void listReviewTasksReturnsWorkbenchItemsFilteredByReviewer() {
        when(admissionReviewTaskRepository.findAllByTaskStatusAndReviewerIdOrderByLastOperatedAtDescTaskSequenceDesc(
                AdmissionReviewTaskStatus.ACTIVE,
                "reviewer-b"
        )).thenReturn(List.of(new AdmissionReviewTaskEntity(
                "app-12",
                2L,
                AdmissionReviewTaskStatus.ACTIVE,
                "reviewer-b",
                OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                OffsetDateTime.parse("2026-04-01T11:30:00+08:00"),
                "ops-lead",
                "转派给复审",
                OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                null,
                3L
        )));
        when(admissionApplicationRepository.findAllById(List.of("app-12")))
                .thenReturn(List.of(new AdmissionApplicationEntity(
                        "app-12",
                        "MERCHANT",
                        AdmissionApplicationStatus.UNDER_REVIEW,
                        "测试商家",
                        "LICENSE-12",
                        "王五",
                        "13700137000",
                        "转派给复审",
                        "reviewer-b",
                        OffsetDateTime.parse("2026-04-01T10:30:00+08:00"),
                        OffsetDateTime.parse("2026-04-01T09:20:00+08:00"),
                        3L
                )));

        List<FoundationQueryModels.AdmissionReviewTaskQueueItemView> actual =
                foundationAdmissionService.listReviewTasks("ACTIVE", "reviewer-b");

        assertThat(actual).singleElement().satisfies(item -> {
            assertThat(item.taskId()).isEqualTo("app-12::2");
            assertThat(item.applicationId()).isEqualTo("app-12");
            assertThat(item.taskSequence()).isEqualTo(2L);
            assertThat(item.taskStatus()).isEqualTo("ACTIVE");
            assertThat(item.reviewerId()).isEqualTo("reviewer-b");
            assertThat(item.organizationType()).isEqualTo("MERCHANT");
            assertThat(item.applicantName()).isEqualTo("测试商家");
            assertThat(item.applicationStatus()).isEqualTo("UNDER_REVIEW");
            assertThat(item.slaStatus()).isEqualTo("NEAR_DUE");
        });
    }

    @Test
    void listPendingApplicationsReturnsSubmittedQueueFilteredByOrganizationType() {
        when(admissionApplicationRepository.findAllByApplicationStatusAndOrganizationTypeOrderBySubmittedAtAsc(
                AdmissionApplicationStatus.SUBMITTED,
                "SUPPLIER"
        )).thenReturn(List.of(new AdmissionApplicationEntity(
                "app-13",
                "SUPPLIER",
                AdmissionApplicationStatus.SUBMITTED,
                "待审供应商",
                "LICENSE-13",
                "李七",
                "13600136007",
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T08:30:00+08:00"),
                1L
        )));

        List<FoundationQueryModels.AdmissionPendingApplicationView> actual =
                foundationAdmissionService.listPendingApplications("SUPPLIER");

        assertThat(actual).singleElement().satisfies(item -> {
            assertThat(item.applicationId()).isEqualTo("app-13");
            assertThat(item.organizationType()).isEqualTo("SUPPLIER");
            assertThat(item.applicantName()).isEqualTo("待审供应商");
            assertThat(item.contactName()).isEqualTo("李七");
            assertThat(item.contactMobile()).isEqualTo("13600136007");
            assertThat(item.applicationStatus()).isEqualTo("SUBMITTED");
        });
    }

    @Test
    void timeoutReviewReturnsExistingApplicationWhenOperationIsRetried() {
        AdmissionApplicationEntity application = new AdmissionApplicationEntity(
                "app-10",
                "MERCHANT",
                AdmissionApplicationStatus.SUBMITTED,
                "测试商家",
                "LICENSE-10",
                "郑十二",
                "13100131000",
                "审核超时退回待分配",
                "ops-supervisor",
                OffsetDateTime.parse("2026-04-01T11:00:00+08:00"),
                OffsetDateTime.parse("2026-04-01T09:20:00+08:00"),
                4L
        );
        when(admissionApplicationRepository.findById("app-10")).thenReturn(Optional.of(application));

        AdmissionApplicationEntity actual = foundationAdmissionService.timeoutReview(
                "app-10",
                new FoundationOperationCommand("ops-supervisor", "审核超时退回待分配")
        );

        assertThat(actual).isSameAs(application);
        assertThat(savedReviewRecords).isEmpty();
        assertThat(savedOperationLogs).isEmpty();
    }
}
