package com.gmall.trade.application;

import com.gmall.trade.infrastructure.persistence.CartItemEntity;
import com.gmall.trade.infrastructure.persistence.CartItemRepository;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionEntity;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeCartService {

    private final CartItemRepository cartItemRepository;
    private final EligibilityProjectionRepository eligibilityProjectionRepository;
    private final PricingInventoryGateway pricingInventoryGateway;

    public TradeCartService(CartItemRepository cartItemRepository,
                            EligibilityProjectionRepository eligibilityProjectionRepository,
                            PricingInventoryGateway pricingInventoryGateway) {
        this.cartItemRepository = cartItemRepository;
        this.eligibilityProjectionRepository = eligibilityProjectionRepository;
        this.pricingInventoryGateway = pricingInventoryGateway;
    }

    @Transactional
    public TradeCartModels.CartItemView addCartItem(TradeCartModels.AddCartItemCommand command) {
        validateAddCommand(command);
        OffsetDateTime now = OffsetDateTime.now();
        CartItemEntity entity = cartItemRepository
                .findByBuyerIdAndBusinessSkuTypeAndBusinessSkuIdAndSellerIdAndStorefrontId(
                        command.buyerId(),
                        command.businessSkuType(),
                        command.businessSkuId(),
                        command.sellerId(),
                        command.storefrontId()
                )
                .map(existing -> {
                    existing.mergeQuantity(command.quantity(), now);
                    return existing;
                })
                .orElseGet(() -> new CartItemEntity(
                        UUID.randomUUID().toString(),
                        command.buyerId(),
                        command.businessSkuType(),
                        command.businessSkuId(),
                        command.sellerId(),
                        command.storefrontId(),
                        command.quantity(),
                        true,
                        now,
                        now
                ));
        return toView(cartItemRepository.save(entity));
    }

    @Transactional
    public TradeCartModels.CartItemView updateCartItem(String cartItemId, TradeCartModels.UpdateCartItemCommand command) {
        if (command == null || command.quantity() <= 0) {
            throw new IllegalArgumentException("购物车数量必须大于 0");
        }
        CartItemEntity entity = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("购物车条目不存在: " + cartItemId));
        entity.update(command.quantity(), command.selected(), OffsetDateTime.now());
        return toView(cartItemRepository.save(entity));
    }

    @Transactional
    public void deleteCartItem(String cartItemId) {
        CartItemEntity entity = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("购物车条目不存在: " + cartItemId));
        cartItemRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public TradeCartModels.CartView getCart(String buyerId) {
        if (buyerId == null || buyerId.isBlank()) {
            throw new IllegalArgumentException("buyerId 不能为空");
        }
        List<TradeCartModels.CartItemView> items = cartItemRepository.findByBuyerIdOrderByUpdatedAtDesc(buyerId).stream()
                .map(this::toView)
                .toList();
        return new TradeCartModels.CartView(buyerId, items);
    }

    @Transactional(readOnly = true)
    public TradeCartModels.CheckoutPreviewView previewCheckout(String buyerId) {
        if (buyerId == null || buyerId.isBlank()) {
            throw new IllegalArgumentException("buyerId 不能为空");
        }
        List<CartItemEntity> selectedItems = cartItemRepository.findByBuyerIdOrderByUpdatedAtDesc(buyerId).stream()
                .filter(CartItemEntity::isSelected)
                .toList();
        Map<String, List<CartItemEntity>> groups = new LinkedHashMap<>();
        for (CartItemEntity item : selectedItems) {
            groups.computeIfAbsent(item.getSellerId(), ignored -> new ArrayList<>()).add(item);
        }
        List<TradeCartModels.SellerCheckoutGroupView> previews = new ArrayList<>();
        long totalAmount = 0L;
        for (Map.Entry<String, List<CartItemEntity>> entry : groups.entrySet()) {
            CartItemEntity first = entry.getValue().get(0);
            EligibilityProjectionEntity eligibility = eligibilityProjectionRepository
                    .findBySellerIdAndStorefrontId(first.getSellerId(), first.getStorefrontId())
                    .filter(EligibilityProjectionEntity::isEligible)
                    .orElseThrow(() -> new OrderRejectedException(
                            buyerId,
                            "ELIGIBILITY_MISSING",
                            "未找到可用经营资格: " + first.getSellerId() + "/" + first.getStorefrontId()
                    ));
            PricingInventoryGateway.QuoteResult quote = pricingInventoryGateway.quoteForOrder(
                    "cart-preview:" + buyerId + ":" + entry.getKey(),
                    entry.getValue().stream()
                            .map(item -> new PricingInventoryGateway.OrderLine(
                                    item.getBusinessSkuType(),
                                    item.getBusinessSkuId(),
                                    item.getQuantity()
                            ))
                            .toList()
            );
            totalAmount += quote.totalAmount();
            Map<String, PricingInventoryGateway.QuotedOrderLine> quoteBySku = new LinkedHashMap<>();
            for (PricingInventoryGateway.QuotedOrderLine quotedOrderLine : quote.items()) {
                quoteBySku.put(businessSkuKey(quotedOrderLine.businessSkuType(), quotedOrderLine.businessSkuId()), quotedOrderLine);
            }
            List<TradeCartModels.CartItemPreviewView> previewItems = entry.getValue().stream()
                    .map(item -> {
                        PricingInventoryGateway.QuotedOrderLine quoted =
                                quoteBySku.get(businessSkuKey(item.getBusinessSkuType(), item.getBusinessSkuId()));
                        long unitPrice = quoted == null ? 0L : quoted.unitPrice();
                        long subtotal = quoted == null ? 0L : quoted.subtotal();
                        return new TradeCartModels.CartItemPreviewView(
                                item.getCartItemId(),
                                item.getBusinessSkuType(),
                                item.getBusinessSkuId(),
                                item.getQuantity(),
                                unitPrice,
                                subtotal
                        );
                    })
                    .toList();
            previews.add(new TradeCartModels.SellerCheckoutGroupView(
                    eligibility.getSellerId(),
                    eligibility.getStorefrontId(),
                    quote.totalAmount(),
                    previewItems
            ));
        }
        return new TradeCartModels.CheckoutPreviewView(buyerId, totalAmount, previews);
    }

    private TradeCartModels.CartItemView toView(CartItemEntity entity) {
        return new TradeCartModels.CartItemView(
                entity.getCartItemId(),
                entity.getBuyerId(),
                entity.getBusinessSkuType(),
                entity.getBusinessSkuId(),
                entity.getSellerId(),
                entity.getStorefrontId(),
                entity.getQuantity(),
                entity.isSelected()
        );
    }

    private void validateAddCommand(TradeCartModels.AddCartItemCommand command) {
        if (command == null
                || command.buyerId() == null || command.buyerId().isBlank()
                || command.businessSkuType() == null || command.businessSkuType().isBlank()
                || command.businessSkuId() == null || command.businessSkuId().isBlank()
                || command.sellerId() == null || command.sellerId().isBlank()
                || command.storefrontId() == null || command.storefrontId().isBlank()) {
            throw new IllegalArgumentException("购物车入参不完整");
        }
        if (command.quantity() <= 0) {
            throw new IllegalArgumentException("购物车数量必须大于 0");
        }
    }

    private String businessSkuKey(String businessSkuType, String businessSkuId) {
        return businessSkuType + "::" + businessSkuId;
    }
}
