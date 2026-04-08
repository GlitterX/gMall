package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.AdmissionReviewTaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionReviewTaskRepository extends JpaRepository<AdmissionReviewTaskEntity, String> {

    long countByTaskStatus(AdmissionReviewTaskStatus taskStatus);

    long countByTaskStatusAndDeadlineAtBefore(AdmissionReviewTaskStatus taskStatus, java.time.OffsetDateTime deadlineAt);

    Optional<AdmissionReviewTaskEntity> findFirstByApplicationIdOrderByTaskSequenceDesc(String applicationId);

    Optional<AdmissionReviewTaskEntity> findFirstByApplicationIdAndTaskStatusOrderByTaskSequenceDesc(String applicationId,
                                                                                                      AdmissionReviewTaskStatus taskStatus);

    List<AdmissionReviewTaskEntity> findAllByTaskStatusOrderByLastOperatedAtDescTaskSequenceDesc(AdmissionReviewTaskStatus taskStatus);

    List<AdmissionReviewTaskEntity> findAllByTaskStatusAndReviewerIdOrderByLastOperatedAtDescTaskSequenceDesc(AdmissionReviewTaskStatus taskStatus,
                                                                                                                String reviewerId);
}
