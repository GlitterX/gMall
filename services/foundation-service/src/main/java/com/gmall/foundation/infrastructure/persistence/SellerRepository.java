package com.gmall.foundation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<SellerEntity, String> {
}
