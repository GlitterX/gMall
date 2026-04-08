package com.gmall.foundation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontRepository extends JpaRepository<StorefrontEntity, String> {
}
