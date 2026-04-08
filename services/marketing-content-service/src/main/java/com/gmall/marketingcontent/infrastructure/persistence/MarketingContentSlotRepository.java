package com.gmall.marketingcontent.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketingContentSlotRepository extends JpaRepository<MarketingContentSlotEntity, String> {

    Optional<MarketingContentSlotEntity> findByOwnerTypeAndOwnerIdAndSlotCode(String ownerType, String ownerId, String slotCode);

    List<MarketingContentSlotEntity> findAllByOrderByUpdatedAtDesc();

    List<MarketingContentSlotEntity> findByOwnerTypeAndOwnerIdAndSlotStatusOrderByUpdatedAtDesc(String ownerType,
                                                                                                  String ownerId,
                                                                                                  String slotStatus);

    List<MarketingContentSlotEntity> findBySlotStatusOrderByUpdatedAtDesc(String slotStatus);
}
