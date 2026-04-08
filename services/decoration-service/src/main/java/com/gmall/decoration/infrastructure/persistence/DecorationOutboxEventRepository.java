package com.gmall.decoration.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecorationOutboxEventRepository extends JpaRepository<DecorationOutboxEventEntity, String> {

    List<DecorationOutboxEventEntity> findByPublishStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(List<String> publishStatuses,
                                                                                                           OffsetDateTime nextRetryAt);
}
