package com.gmall.marketingcontent.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicContentBlockRepository extends JpaRepository<TopicContentBlockEntity, String> {

    Optional<TopicContentBlockEntity> findByOwnerTypeAndOwnerIdAndTopicCode(String ownerType, String ownerId, String topicCode);

    List<TopicContentBlockEntity> findAllByOrderByUpdatedAtDesc();

    List<TopicContentBlockEntity> findByOwnerTypeAndOwnerIdAndTopicStatusOrderByUpdatedAtDesc(String ownerType,
                                                                                                String ownerId,
                                                                                                String topicStatus);

    List<TopicContentBlockEntity> findByTopicStatusOrderByUpdatedAtDesc(String topicStatus);
}
