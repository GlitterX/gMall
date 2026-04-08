package com.gmall.catalog.infrastructure.persistence;

import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findByPublishStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(List<String> publishStatuses,
                                                                                                  OffsetDateTime nextRetryAt);
}
