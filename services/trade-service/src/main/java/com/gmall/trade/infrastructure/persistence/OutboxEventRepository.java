package com.gmall.trade.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findByPublishedAtIsNullAndAggregateTypeOrderByCreatedAtAsc(String aggregateType);

    Optional<OutboxEventEntity> findByAggregateTypeAndAggregateIdAndEventType(String aggregateType,
                                                                              String aggregateId,
                                                                              String eventType);
}
