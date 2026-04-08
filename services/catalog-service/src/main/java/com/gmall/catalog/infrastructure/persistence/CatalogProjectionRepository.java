package com.gmall.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogProjectionRepository extends JpaRepository<CatalogProjectionEntity, String> {

    Optional<CatalogProjectionEntity> findByPresentationTypeAndProductViewIdAndLocale(String presentationType,
                                                                                       String productViewId,
                                                                                       String locale);

    List<CatalogProjectionEntity> findByPresentationTypeAndProductViewIdOrderByUpdatedAtDesc(String presentationType,
                                                                                              String productViewId);
}
