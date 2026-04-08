package com.gmall.catalog.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceSkuRepository extends JpaRepository<SourceSkuEntity, String> {

    List<SourceSkuEntity> findBySourceProductId(String sourceProductId);
}
