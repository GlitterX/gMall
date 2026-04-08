package com.gmall.catalog.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceProductRepository extends JpaRepository<SourceProductEntity, String> {

    List<SourceProductEntity> findByOwnerId(String ownerId);

    List<SourceProductEntity> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);
}
