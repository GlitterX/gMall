package com.gmall.foundation.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionOperationLogRepository extends JpaRepository<AdmissionOperationLogEntity, String> {

    Optional<AdmissionOperationLogEntity> findByApplicationIdAndActionTypeAndRequestFingerprint(String applicationId,
                                                                                                 String actionType,
                                                                                                 String requestFingerprint);
}
