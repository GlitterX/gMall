package com.gmall.foundation.application;

import com.gmall.foundation.domain.model.AdmissionApplicationStatus;
import com.gmall.foundation.domain.model.AdmissionReviewTaskStatus;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationRepository;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewTaskEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewTaskRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdmissionReviewTaskSupport {

    private static final long REVIEW_TIMEOUT_HOURS = 24L;
    private static final long REVIEW_NEAR_DUE_HOURS = 4L;
    private static final String SLA_STATUS_CLOSED = "CLOSED";
    private static final String SLA_STATUS_NEAR_DUE = "NEAR_DUE";
    private static final String SLA_STATUS_ON_TRACK = "ON_TRACK";
    private static final String SLA_STATUS_OVERDUE = "OVERDUE";

    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final AdmissionReviewTaskRepository admissionReviewTaskRepository;
    private final Clock clock;

    @Autowired
    public AdmissionReviewTaskSupport(AdmissionReviewTaskRepository admissionReviewTaskRepository,
                                      AdmissionApplicationRepository admissionApplicationRepository) {
        this(admissionReviewTaskRepository, admissionApplicationRepository, Clock.systemDefaultZone());
    }

    AdmissionReviewTaskSupport(AdmissionReviewTaskRepository admissionReviewTaskRepository,
                               AdmissionApplicationRepository admissionApplicationRepository,
                               Clock clock) {
        this.admissionReviewTaskRepository = admissionReviewTaskRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.clock = clock;
    }

    public void createTask(AdmissionApplicationEntity application,
                           String reviewerId,
                           String operatorId,
                           String operationReason,
                           OffsetDateTime operatedAt) {
        admissionReviewTaskRepository.save(newTask(
                application.getApplicationId(),
                nextTaskSequence(application.getApplicationId()),
                reviewerId,
                operatorId,
                operationReason,
                operatedAt
        ));
    }

    public void reassignTask(AdmissionApplicationEntity application,
                             String reviewerId,
                             String operatorId,
                             String operationReason,
                             OffsetDateTime operatedAt) {
        AdmissionReviewTaskEntity task = findActiveTask(application)
                .orElseGet(() -> materializeActiveTask(application));
        task.reassign(reviewerId, operatorId, operationReason, operatedAt, operatedAt.plusHours(REVIEW_TIMEOUT_HOURS));
        admissionReviewTaskRepository.save(task);
    }

    public void completeTask(AdmissionApplicationEntity application,
                             AdmissionReviewTaskStatus taskStatus,
                             String operatorId,
                             String operationReason,
                             OffsetDateTime operatedAt) {
        AdmissionReviewTaskEntity task = findActiveTask(application)
                .orElseGet(() -> materializeActiveTask(application));
        task.close(taskStatus, operatorId, operationReason, operatedAt);
        admissionReviewTaskRepository.save(task);
    }

    public FoundationQueryModels.AdmissionReviewTaskView getReviewTask(String applicationId) {
        AdmissionReviewTaskEntity task = latestTask(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("审核任务不存在: " + applicationId));
        return new FoundationQueryModels.AdmissionReviewTaskView(
                task.getTaskId(),
                task.getApplicationId(),
                task.getTaskSequence(),
                task.getTaskStatus().name(),
                task.getReviewerId(),
                task.getStartedAt(),
                task.getDeadlineAt(),
                task.getClosedAt(),
                task.getLastOperatedBy(),
                task.getLastOperationReason(),
                task.getLastOperatedAt(),
                task.getAggregateVersion()
        );
    }

    public List<FoundationQueryModels.AdmissionReviewTaskQueueItemView> listReviewTasks(String taskStatus, String reviewerId) {
        List<AdmissionReviewTaskEntity> tasks = queryTasks(parseTaskStatus(taskStatus), reviewerId);
        if (tasks.isEmpty()) {
            return List.of();
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        Map<String, AdmissionApplicationEntity> applications = loadApplications(tasks);
        return tasks.stream()
                .map(task -> toWorkbenchItem(task, requireApplication(applications, task.getApplicationId()), now))
                .toList();
    }

    public FoundationQueryModels.AdmissionWorkbenchOverviewView getWorkbenchOverview() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<AdmissionReviewTaskEntity> activeTasks =
                admissionReviewTaskRepository.findAllByTaskStatusOrderByLastOperatedAtDescTaskSequenceDesc(AdmissionReviewTaskStatus.ACTIVE);
        return new FoundationQueryModels.AdmissionWorkbenchOverviewView(
                admissionApplicationRepository.countByApplicationStatus(AdmissionApplicationStatus.SUBMITTED),
                activeTasks.size(),
                activeTasks.stream().filter(task -> isNearDue(task, now)).count(),
                activeTasks.stream().filter(task -> isOverdue(task, now)).count()
        );
    }

    private Optional<AdmissionReviewTaskEntity> findActiveTask(AdmissionApplicationEntity application) {
        Optional<AdmissionReviewTaskEntity> task = admissionReviewTaskRepository
                .findFirstByApplicationIdAndTaskStatusOrderByTaskSequenceDesc(application.getApplicationId(), AdmissionReviewTaskStatus.ACTIVE);
        return task == null ? Optional.empty() : task;
    }

    private Optional<AdmissionReviewTaskEntity> latestTask(String applicationId) {
        Optional<AdmissionReviewTaskEntity> task = admissionReviewTaskRepository
                .findFirstByApplicationIdOrderByTaskSequenceDesc(applicationId);
        return task == null ? Optional.empty() : task;
    }

    private List<AdmissionReviewTaskEntity> queryTasks(AdmissionReviewTaskStatus taskStatus, String reviewerId) {
        if (reviewerId == null || reviewerId.isBlank()) {
            return admissionReviewTaskRepository.findAllByTaskStatusOrderByLastOperatedAtDescTaskSequenceDesc(taskStatus);
        }
        return admissionReviewTaskRepository.findAllByTaskStatusAndReviewerIdOrderByLastOperatedAtDescTaskSequenceDesc(taskStatus, reviewerId);
    }

    private long nextTaskSequence(String applicationId) {
        return latestTask(applicationId)
                .map(AdmissionReviewTaskEntity::getTaskSequence)
                .orElse(0L) + 1;
    }

    private AdmissionReviewTaskStatus parseTaskStatus(String taskStatus) {
        String candidate = taskStatus == null || taskStatus.isBlank()
                ? AdmissionReviewTaskStatus.ACTIVE.name()
                : taskStatus.trim().toUpperCase(Locale.ROOT);
        try {
            return AdmissionReviewTaskStatus.valueOf(candidate);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的审核任务状态: " + taskStatus);
        }
    }

    private Map<String, AdmissionApplicationEntity> loadApplications(List<AdmissionReviewTaskEntity> tasks) {
        List<String> applicationIds = tasks.stream()
                .map(AdmissionReviewTaskEntity::getApplicationId)
                .distinct()
                .toList();
        return admissionApplicationRepository.findAllById(applicationIds).stream()
                .collect(Collectors.toMap(AdmissionApplicationEntity::getApplicationId, Function.identity()));
    }

    private AdmissionApplicationEntity requireApplication(Map<String, AdmissionApplicationEntity> applications, String applicationId) {
        AdmissionApplicationEntity application = applications.get(applicationId);
        if (application == null) {
            throw new IllegalStateException("入驻申请不存在: " + applicationId);
        }
        return application;
    }

    private FoundationQueryModels.AdmissionReviewTaskQueueItemView toWorkbenchItem(AdmissionReviewTaskEntity task,
                                                                                   AdmissionApplicationEntity application,
                                                                                   OffsetDateTime now) {
        return new FoundationQueryModels.AdmissionReviewTaskQueueItemView(
                task.getTaskId(),
                task.getApplicationId(),
                task.getTaskSequence(),
                task.getTaskStatus().name(),
                task.getReviewerId(),
                task.getDeadlineAt(),
                task.getLastOperatedAt(),
                application.getOrganizationType(),
                application.getApplicantName(),
                application.getApplicationStatus().name(),
                resolveSlaStatus(task, now)
        );
    }

    private String resolveSlaStatus(AdmissionReviewTaskEntity task, OffsetDateTime now) {
        if (task.getTaskStatus() != AdmissionReviewTaskStatus.ACTIVE) {
            return SLA_STATUS_CLOSED;
        }
        if (isOverdue(task, now)) {
            return SLA_STATUS_OVERDUE;
        }
        if (isNearDue(task, now)) {
            return SLA_STATUS_NEAR_DUE;
        }
        return SLA_STATUS_ON_TRACK;
    }

    private boolean isNearDue(AdmissionReviewTaskEntity task, OffsetDateTime now) {
        OffsetDateTime deadlineAt = task.getDeadlineAt();
        OffsetDateTime alertThreshold = now.plusHours(REVIEW_NEAR_DUE_HOURS);
        return deadlineAt.isAfter(now) && !deadlineAt.isAfter(alertThreshold);
    }

    private boolean isOverdue(AdmissionReviewTaskEntity task, OffsetDateTime now) {
        return !task.getDeadlineAt().isAfter(now);
    }

    private AdmissionReviewTaskEntity materializeActiveTask(AdmissionApplicationEntity application) {
        if (application.getApplicationStatus() != AdmissionApplicationStatus.UNDER_REVIEW
                || application.getReviewedBy() == null
                || application.getReviewedAt() == null) {
            throw new IllegalStateException("审核任务不存在: " + application.getApplicationId());
        }
        return newTask(
                application.getApplicationId(),
                nextTaskSequence(application.getApplicationId()),
                application.getReviewedBy(),
                application.getReviewedBy(),
                application.getReviewComment() == null ? "开始审核" : application.getReviewComment(),
                application.getReviewedAt()
        );
    }

    private AdmissionReviewTaskEntity newTask(String applicationId,
                                              long taskSequence,
                                              String reviewerId,
                                              String operatorId,
                                              String operationReason,
                                              OffsetDateTime operatedAt) {
        return new AdmissionReviewTaskEntity(
                applicationId,
                taskSequence,
                AdmissionReviewTaskStatus.ACTIVE,
                reviewerId,
                operatedAt,
                operatedAt.plusHours(REVIEW_TIMEOUT_HOURS),
                operatorId,
                operationReason,
                operatedAt,
                null,
                1L
        );
    }
}
