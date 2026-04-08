package com.gmall.decoration.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecorationPublishRecordRepository extends JpaRepository<DecorationPublishRecordEntity, String> {

    Optional<DecorationPublishRecordEntity> findByOperationRequestId(String operationRequestId);
}
