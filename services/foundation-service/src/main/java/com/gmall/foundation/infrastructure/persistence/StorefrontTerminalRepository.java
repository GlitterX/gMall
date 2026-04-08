package com.gmall.foundation.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontTerminalRepository extends JpaRepository<StorefrontTerminalEntity, String> {

    List<StorefrontTerminalEntity> findByStorefrontIdOrderByTerminalTypeAsc(String storefrontId);
}
