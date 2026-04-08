package com.gmall.foundation.infrastructure.persistence;

import com.gmall.foundation.domain.model.AdmissionApplicationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplicationEntity, String> {

    long countByApplicationStatus(AdmissionApplicationStatus applicationStatus);

    List<AdmissionApplicationEntity> findAllByApplicationStatusOrderBySubmittedAtAsc(AdmissionApplicationStatus applicationStatus);

    List<AdmissionApplicationEntity> findAllByApplicationStatusAndOrganizationTypeOrderBySubmittedAtAsc(AdmissionApplicationStatus applicationStatus,
                                                                                                         String organizationType);
}
