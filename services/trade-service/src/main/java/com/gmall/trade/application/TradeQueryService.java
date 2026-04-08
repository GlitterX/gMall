package com.gmall.trade.application;

import com.gmall.trade.infrastructure.persistence.EligibilityProjectionEntity;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionRepository;
import com.gmall.trade.infrastructure.persistence.OrderEntity;
import com.gmall.trade.infrastructure.persistence.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeQueryService {

    private final OrderRepository orderRepository;
    private final EligibilityProjectionRepository eligibilityProjectionRepository;
    private final PricingInventoryGateway pricingInventoryGateway;

    public TradeQueryService(OrderRepository orderRepository,
                             EligibilityProjectionRepository eligibilityProjectionRepository,
                             PricingInventoryGateway pricingInventoryGateway) {
        this.orderRepository = orderRepository;
        this.eligibilityProjectionRepository = eligibilityProjectionRepository;
        this.pricingInventoryGateway = pricingInventoryGateway;
    }

    @Transactional(readOnly = true)
    public TradeQueryModels.OrderView getOrder(String orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        return new TradeQueryModels.OrderView(
                order.getOrderId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getSellerOfRecord(),
                order.getSettlementBeneficiary(),
                order.getOrderScene(),
                order.getStorefrontId(),
                order.getPayableAmount(),
                order.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public TradeQueryModels.EligibilityView getEligibility(String sellerId, String storefrontId) {
        EligibilityProjectionEntity projection = eligibilityProjectionRepository
                .findBySellerIdAndStorefrontId(sellerId, storefrontId)
                .orElseThrow(() -> new EligibilityNotFoundException(sellerId, storefrontId));
        return new TradeQueryModels.EligibilityView(
                projection.getSellerId(),
                projection.getStorefrontId(),
                projection.getOrganizationId(),
                projection.isEligible()
        );
    }

    @Transactional(readOnly = true)
    public PricingInventoryGateway.PricingSnapshotView getOrderPricingSnapshot(String orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        return pricingInventoryGateway.getSnapshot(extractBusinessKey(order.getPricingSnapshotRef()));
    }

    private String extractBusinessKey(String pricingSnapshotRef) {
        String prefix = "pricing-snapshot:";
        if (pricingSnapshotRef != null && pricingSnapshotRef.startsWith(prefix)) {
            return pricingSnapshotRef.substring(prefix.length());
        }
        return pricingSnapshotRef;
    }
}
