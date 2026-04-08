package com.gmall.trade.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.gmall.trade.infrastructure.messaging.EventEnvelope;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionEntity;
import com.gmall.trade.infrastructure.persistence.EligibilityProjectionRepository;
import com.gmall.trade.infrastructure.persistence.OrderEntity;
import com.gmall.trade.infrastructure.persistence.OrderRepository;
import com.gmall.trade.infrastructure.persistence.OutboxEventEntity;
import com.gmall.trade.infrastructure.persistence.OutboxEventRepository;
import com.gmall.trade.infrastructure.persistence.SubOrderEntity;
import com.gmall.trade.infrastructure.persistence.SubOrderRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitOrderSagaService {

    private static final String DIRECT_ORDER_SCENE = "DIRECT_SUPPLIER";
    private static final String MERCHANT_SUPPLY_ORDER_SCENE = "MERCHANT_SUPPLY";
    private static final String CURRENCY_CODE = "CNY";
    private static final String ORDER_AGGREGATE_TYPE = "Order";
    private static final String ORDER_SUBMITTED_EVENT = "OrderSubmitted";
    private static final String ORDER_REJECTED_EVENT = "OrderRejected";
    private static final String ELIGIBILITY_MISSING = "ELIGIBILITY_MISSING";

    private final EligibilityProjectionRepository eligibilityProjectionRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PricingInventoryGateway pricingInventoryGateway;
    private final SubmitOrderSagaHook submitOrderSagaHook;
    private final ObjectMapper objectMapper;

    public SubmitOrderSagaService(EligibilityProjectionRepository eligibilityProjectionRepository,
                                  OrderRepository orderRepository,
                                  SubOrderRepository subOrderRepository,
                                  OutboxEventRepository outboxEventRepository,
                                  PricingInventoryGateway pricingInventoryGateway,
                                  SubmitOrderSagaHook submitOrderSagaHook,
                                  ObjectMapper objectMapper) {
        this.eligibilityProjectionRepository = eligibilityProjectionRepository;
        this.orderRepository = orderRepository;
        this.subOrderRepository = subOrderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.pricingInventoryGateway = pricingInventoryGateway;
        this.submitOrderSagaHook = submitOrderSagaHook;
        this.objectMapper = objectMapper;
    }

    @Transactional(noRollbackFor = OrderRejectedException.class)
    public SubmitOrderResult submit(SubmitOrderCommand command) {
        List<OrderEntity> existingOrders = orderRepository
                .findByBusinessIdempotencyKeyOrderByOrderNoAsc(command.businessIdempotencyKey());
        if (!existingOrders.isEmpty()) {
            return new SubmitOrderResult(existingOrders.stream().map(OrderEntity::getOrderId).toList());
        }

        outboxEventRepository.findByAggregateTypeAndAggregateIdAndEventType(
                        ORDER_AGGREGATE_TYPE,
                        command.businessIdempotencyKey(),
                        ORDER_REJECTED_EVENT
                )
                .ifPresent(event -> {
                    throw readRejectedException(event);
                });

        List<PreparedSellerOrderGroup> preparedGroups = new ArrayList<>();
        try {
            Map<String, SellerOrderGroup> groupedItems = groupBySeller(command);
            prepareSellerGroups(command, groupedItems, preparedGroups);
            SubmitOrderSagaContext context = new SubmitOrderSagaContext(
                    command.businessIdempotencyKey(),
                    command.buyerId(),
                    new ArrayList<>()
            );

            submitOrderSagaHook.afterStep(SubmitOrderSagaStep.ELIGIBILITY_VALIDATED, context);

            int sequence = 1;
            for (PreparedSellerOrderGroup preparedGroup : preparedGroups) {
                OrderEntity order = orderRepository.save(createOrder(command, preparedGroup, sequence));
                subOrderRepository.save(createSubOrder(order, preparedGroup.group()));
                context.orderIds().add(order.getOrderId());
                sequence++;
            }
            submitOrderSagaHook.afterStep(SubmitOrderSagaStep.ORDERS_CREATED, context);

            for (String orderId : context.orderIds()) {
                appendOutboxEvent(orderId, command.businessIdempotencyKey());
            }
            submitOrderSagaHook.afterStep(SubmitOrderSagaStep.EVENTS_APPENDED, context);

            return new SubmitOrderResult(List.copyOf(context.orderIds()));
        } catch (RuntimeException exception) {
            releasePreparedReservations(preparedGroups, command.businessIdempotencyKey());
            if (exception instanceof OrderRejectedException rejectedException) {
                appendRejectedEventIfAbsent(command, rejectedException);
            }
            throw exception;
        }
    }

    private Map<String, SellerOrderGroup> groupBySeller(SubmitOrderCommand command) {
        Map<String, SellerOrderGroup> groupedItems = new LinkedHashMap<>();
        for (SubmitOrderItem item : command.items()) {
            EligibilityProjectionEntity projection = eligibilityProjectionRepository
                    .findBySellerIdAndStorefrontId(item.sellerId(), item.storefrontId())
                    .filter(EligibilityProjectionEntity::isEligible)
                    .orElseThrow(() -> new OrderRejectedException(
                            command.businessIdempotencyKey(),
                            ELIGIBILITY_MISSING,
                            "未找到可用经营资格: " + item.sellerId() + "/" + item.storefrontId()
                    ));
            groupedItems.computeIfAbsent(item.sellerId(),
                    ignored -> new SellerOrderGroup(projection, new ArrayList<>()))
                    .items()
                    .add(item);
        }
        return groupedItems;
    }

    public PricingInventoryGateway.InventoryReservationResult confirmOrderInventory(String orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));
        return pricingInventoryGateway.confirmInventory(order.getInventoryReservationRef(), "confirm:" + orderId);
    }

    private void prepareSellerGroups(SubmitOrderCommand command,
                                     Map<String, SellerOrderGroup> groupedItems,
                                     List<PreparedSellerOrderGroup> preparedGroups) {
        for (Map.Entry<String, SellerOrderGroup> entry : groupedItems.entrySet()) {
            String pricingBusinessKey = pricingBusinessKey(command.businessIdempotencyKey(), entry.getKey());
            List<PricingInventoryGateway.OrderLine> orderLines = entry.getValue().items().stream()
                    .map(item -> new PricingInventoryGateway.OrderLine(
                            item.businessSkuType(),
                            item.businessSkuId(),
                            item.quantity()
                    ))
                    .toList();
            PricingInventoryGateway.InventoryReservationResult reservationResult =
                    pricingInventoryGateway.reserveInventory(pricingBusinessKey, orderLines);
            preparedGroups.add(new PreparedSellerOrderGroup(entry.getValue(), reservationResult));
        }
    }

    private void releasePreparedReservations(List<PreparedSellerOrderGroup> preparedGroups, String businessIdempotencyKey) {
        for (PreparedSellerOrderGroup preparedGroup : preparedGroups) {
            pricingInventoryGateway.releaseInventory(
                    preparedGroup.reservationResult().inventoryReservationRef(),
                    "release:" + pricingBusinessKey(businessIdempotencyKey, preparedGroup.group().projection().getSellerId())
            );
        }
    }

    private OrderEntity createOrder(SubmitOrderCommand command, PreparedSellerOrderGroup preparedGroup, int sequence) {
        SellerOrderGroup group = preparedGroup.group();
        OrderSceneResolution orderScene = resolveOrderScene(group.items());
        long payableAmount = preparedGroup.reservationResult().totalAmount();
        String orderId = UUID.randomUUID().toString();
        return new OrderEntity(
                orderId,
                "ORD-" + command.businessIdempotencyKey() + "-" + sequence,
                command.businessIdempotencyKey(),
                command.buyerId(),
                group.projection().getSellerId(),
                orderScene.sellerType(),
                group.projection().getSellerId(),
                group.projection().getOrganizationId(),
                orderScene.orderScene(),
                group.projection().getStorefrontId(),
                CURRENCY_CODE,
                payableAmount,
                0L,
                0L,
                payableAmount,
                "{\"receiver\":\"pending\"}",
                preparedGroup.reservationResult().pricingSnapshotRef(),
                preparedGroup.reservationResult().inventoryReservationRef(),
                "SUBMITTED",
                OffsetDateTime.now()
        );
    }

    private SubOrderEntity createSubOrder(OrderEntity order, SellerOrderGroup group) {
        try {
            return new SubOrderEntity(
                    UUID.randomUUID().toString(),
                    order.getOrderId(),
                    group.projection().getSellerId(),
                    group.projection().getStorefrontId(),
                    objectMapper.writeValueAsString(group.items())
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化子订单快照", exception);
        }
    }

    private void appendOutboxEvent(String orderId, String businessIdempotencyKey) {
        try {
            outboxEventRepository.save(
                    new OutboxEventEntity(
                            UUID.randomUUID().toString(),
                            ORDER_AGGREGATE_TYPE,
                            orderId,
                            ORDER_SUBMITTED_EVENT,
                            objectMapper.writeValueAsString(
                                    new EventEnvelope<>(
                                            UUID.randomUUID().toString(),
                                            ORDER_SUBMITTED_EVENT,
                                            ORDER_AGGREGATE_TYPE,
                                            orderId,
                                            1L,
                                            OffsetDateTime.now(),
                                            businessIdempotencyKey,
                                            Map.of("orderId", orderId, "businessIdempotencyKey", businessIdempotencyKey)
                                    )
                            ),
                            OffsetDateTime.now(),
                            null
                    )
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法写入订单 outbox 事件", exception);
        }
    }

    private void appendRejectedEventIfAbsent(SubmitOrderCommand command, OrderRejectedException exception) {
        if (outboxEventRepository.findByAggregateTypeAndAggregateIdAndEventType(
                ORDER_AGGREGATE_TYPE,
                command.businessIdempotencyKey(),
                ORDER_REJECTED_EVENT
        ).isPresent()) {
            return;
        }
        try {
            outboxEventRepository.save(
                    new OutboxEventEntity(
                            UUID.randomUUID().toString(),
                            ORDER_AGGREGATE_TYPE,
                            command.businessIdempotencyKey(),
                            ORDER_REJECTED_EVENT,
                            objectMapper.writeValueAsString(
                                    new EventEnvelope<>(
                                            UUID.randomUUID().toString(),
                                            ORDER_REJECTED_EVENT,
                                            ORDER_AGGREGATE_TYPE,
                                            command.businessIdempotencyKey(),
                                            1L,
                                            OffsetDateTime.now(),
                                            command.businessIdempotencyKey(),
                                            Map.of(
                                                    "businessIdempotencyKey", command.businessIdempotencyKey(),
                                                    "buyerId", command.buyerId(),
                                                    "reasonCode", exception.reasonCode(),
                                                    "reasonMessage", exception.getMessage()
                                            )
                                    )
                            ),
                            OffsetDateTime.now(),
                            null
                    )
            );
        } catch (JsonProcessingException jsonProcessingException) {
            throw new IllegalStateException("无法写入拒单 outbox 事件", jsonProcessingException);
        }
    }

    private OrderRejectedException readRejectedException(OutboxEventEntity event) {
        try {
            JsonNode root = objectMapper.readTree(event.getPayload());
            JsonNode payload = root.get("payload");
            return new OrderRejectedException(
                    payload.get("businessIdempotencyKey").asText(),
                    payload.get("reasonCode").asText(),
                    payload.get("reasonMessage").asText()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析拒单 outbox 事件", exception);
        }
    }

    private record SellerOrderGroup(EligibilityProjectionEntity projection, List<SubmitOrderItem> items) {
    }

    private record PreparedSellerOrderGroup(SellerOrderGroup group,
                                            PricingInventoryGateway.InventoryReservationResult reservationResult) {
    }

    private record OrderSceneResolution(String orderScene, String sellerType) {
    }

    private String pricingBusinessKey(String businessIdempotencyKey, String sellerId) {
        return businessIdempotencyKey + ":" + sellerId;
    }

    private OrderSceneResolution resolveOrderScene(List<SubmitOrderItem> items) {
        boolean hasDirectSku = items.stream().anyMatch(item -> "SOURCE_SKU".equals(item.businessSkuType()));
        boolean hasMerchantOfferSku = items.stream().anyMatch(item -> "MERCHANT_OFFER_SKU".equals(item.businessSkuType()));
        if (hasDirectSku && hasMerchantOfferSku) {
            throw new OrderRejectedException(
                    "MIXED_ORDER_SCENE",
                    "MIXED_ORDER_SCENE",
                    "同一卖家分组下不允许混合直营 SKU 与经营 SKU"
            );
        }
        if (hasDirectSku) {
            return new OrderSceneResolution(DIRECT_ORDER_SCENE, "DIRECT_SUPPLIER");
        }
        return new OrderSceneResolution(MERCHANT_SUPPLY_ORDER_SCENE, "MERCHANT");
    }
}
