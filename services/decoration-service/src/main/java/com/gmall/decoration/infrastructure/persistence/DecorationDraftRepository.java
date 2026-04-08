package com.gmall.decoration.infrastructure.persistence;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecorationDraftRepository extends JpaRepository<DecorationDraftEntity, String> {

    Optional<DecorationDraftEntity> findFirstByPageIdAndDraftStatusInOrderByDraftVersionDesc(String pageId,
                                                                                              Collection<String> draftStatuses);

    Optional<DecorationDraftEntity> findFirstByPageIdOrderByDraftVersionDesc(String pageId);
}
