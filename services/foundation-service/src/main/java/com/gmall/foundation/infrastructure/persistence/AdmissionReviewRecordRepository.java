package com.gmall.foundation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionReviewRecordRepository extends JpaRepository<AdmissionReviewRecordEntity, String> {

    List<AdmissionReviewRecordEntity> findByApplicationIdOrderByOperatedAtAsc(String applicationId);
}
