package com.gmall.decoration.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecorationSnapshotRepository extends JpaRepository<DecorationSnapshotEntity, String> {

    Optional<DecorationSnapshotEntity> findFirstByPageIdAndTerminalTypeOrderBySnapshotVersionDesc(String pageId,
                                                                                                   String terminalType);
}
