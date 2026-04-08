package com.gmall.trade.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItemEntity, String> {

    Optional<CartItemEntity> findByBuyerIdAndBusinessSkuTypeAndBusinessSkuIdAndSellerIdAndStorefrontId(String buyerId,
                                                                                                         String businessSkuType,
                                                                                                         String businessSkuId,
                                                                                                         String sellerId,
                                                                                                         String storefrontId);

    List<CartItemEntity> findByBuyerIdOrderByUpdatedAtDesc(String buyerId);
}
