package com.gmall.decoration.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecorationPageRepository extends JpaRepository<DecorationPageEntity, String> {

    Optional<DecorationPageEntity> findByStorefrontIdAndPageCodeAndTerminalType(String storefrontId,
                                                                                 String pageCode,
                                                                                 String terminalType);

    List<DecorationPageEntity> findByStorefrontIdOrderByCreatedAtDesc(String storefrontId);
}
