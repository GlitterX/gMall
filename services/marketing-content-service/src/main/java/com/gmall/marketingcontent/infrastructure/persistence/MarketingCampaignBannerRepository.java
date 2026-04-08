package com.gmall.marketingcontent.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketingCampaignBannerRepository extends JpaRepository<MarketingCampaignBannerEntity, String> {

    Optional<MarketingCampaignBannerEntity> findByOwnerTypeAndOwnerIdAndCampaignCode(String ownerType,
                                                                                      String ownerId,
                                                                                      String campaignCode);

    List<MarketingCampaignBannerEntity> findAllByOrderByUpdatedAtDesc();

    List<MarketingCampaignBannerEntity> findByOwnerTypeAndOwnerIdAndCampaignStatusOrderByUpdatedAtDesc(String ownerType,
                                                                                                         String ownerId,
                                                                                                         String campaignStatus);

    List<MarketingCampaignBannerEntity> findByCampaignStatusOrderByUpdatedAtDesc(String campaignStatus);
}
