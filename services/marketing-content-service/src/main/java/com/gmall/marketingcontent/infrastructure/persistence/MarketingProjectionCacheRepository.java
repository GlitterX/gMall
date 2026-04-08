package com.gmall.marketingcontent.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketingProjectionCacheRepository extends JpaRepository<MarketingProjectionCacheEntity, String> {

    Optional<MarketingProjectionCacheEntity> findByProjectionTypeAndOwnerTypeAndOwnerIdAndTerminalTypeAndPageContextAndLocale(
            String projectionType,
            String ownerType,
            String ownerId,
            String terminalType,
            String pageContext,
            String locale
    );

    void deleteByOwnerTypeAndOwnerId(String ownerType, String ownerId);
}
