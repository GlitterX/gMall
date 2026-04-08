package com.gmall.trade.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitOrderSagaServiceTest {

    private final EligibilityProjectionRepository eligibilityProjectionRepository = mock(EligibilityProjectionRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final SubOrderRepository subOrderRepository = mock(SubOrderRepository.class);
    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final PricingInventoryGateway pricingInventoryGateway = mock(PricingInventoryGateway.class);

    private final List<OrderEntity> savedOrders = new ArrayList<>();
    private final List<SubOrderEntity> savedSubOrders = new ArrayList<>();
    private final List<OutboxEventEntity> savedOutboxEvents = new ArrayList<>();

    private SubmitOrderSagaService submitOrderSagaService;

    @BeforeEach
    void setUp() {
        submitOrderSagaService = new SubmitOrderSagaService(
                eligibilityProjectionRepository,
                orderRepository,
                subOrderRepository,
                outboxEventRepository,
                pricingInventoryGateway,
                new NoopSubmitOrderSagaHook(),
                new ObjectMapper().findAndRegisterModules()
        );
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("seller-1", "store-1"))
                .thenReturn(Optional.of(new EligibilityProjectionEntity(
                        "seller-1",
                        "store-1",
                        "org-1",
                        true,
                        OffsetDateTime.now()
                )));
        when(orderRepository.findByBusinessIdempotencyKeyOrderByOrderNoAsc("biz-1"))
                .thenAnswer(invocation -> List.copyOf(savedOrders));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> {
                    OrderEntity order = invocation.getArgument(0);
                    savedOrders.add(order);
                    return order;
                });
        when(subOrderRepository.save(any(SubOrderEntity.class)))
                .thenAnswer(invocation -> {
                    SubOrderEntity subOrder = invocation.getArgument(0);
                    savedSubOrders.add(subOrder);
                    return subOrder;
                });
        when(outboxEventRepository.save(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> {
                    OutboxEventEntity event = invocation.getArgument(0);
                    savedOutboxEvents.add(event);
                    return event;
                });
        when(outboxEventRepository.findByPublishedAtIsNullAndAggregateTypeOrderByCreatedAtAsc("Order"))
                .thenAnswer(invocation -> List.copyOf(savedOutboxEvents));
        when(outboxEventRepository.findByAggregateTypeAndAggregateIdAndEventType("Order", "biz-1", "OrderRejected"))
                .thenAnswer(invocation -> savedOutboxEvents.stream()
                        .filter(event -> "OrderRejected".equals(event.getEventType()))
                        .findFirst());
        when(outboxEventRepository.findByAggregateTypeAndAggregateIdAndEventType("Order", "biz-rejected", "OrderRejected"))
                .thenAnswer(invocation -> savedOutboxEvents.stream()
                        .filter(event -> "OrderRejected".equals(event.getEventType()))
                        .findFirst());
        when(pricingInventoryGateway.reserveInventory(eq("biz-1:seller-1"), any()))
                .thenReturn(new PricingInventoryGateway.InventoryReservationResult(
                        "inventory-reservation:biz-1:seller-1",
                        "pricing-snapshot:biz-1:seller-1",
                        3998L,
                        "SELLABLE",
                        List.of(new PricingInventoryGateway.QuotedOrderLine(
                                "MERCHANT_OFFER_SKU",
                                "sku-1",
                                "source-sku-1",
                                1500L,
                                null,
                                1999L,
                                1999L,
                                3998L,
                                "SELLABLE",
                                null
                        ))
                ));
    }

    @Test
    void submitReturnsExistingOrderWhenBusinessIdempotencyKeyAlreadyExists() {
        SubmitOrderCommand command = new SubmitOrderCommand(
                "biz-1",
                "buyer-1",
                "zh-CN",
                List.of(new SubmitOrderItem("MERCHANT_OFFER_SKU", "sku-1", "seller-1", "store-1", 2, 1999))
        );

        SubmitOrderResult first = submitOrderSagaService.submit(command);
        SubmitOrderResult second = submitOrderSagaService.submit(command);

        assertThat(second.orderIds()).containsExactlyElementsOf(first.orderIds());
        assertThat(savedOrders).hasSize(1);
        assertThat(savedSubOrders).hasSize(1);
        assertThat(savedOutboxEvents).hasSize(1);
    }

    @Test
    void submitUsesPricingSnapshotAndReservationRefsBeforeCreatingOrder() {
        SubmitOrderCommand command = new SubmitOrderCommand(
                "biz-1",
                "buyer-1",
                "zh-CN",
                List.of(new SubmitOrderItem("MERCHANT_OFFER_SKU", "sku-1", "seller-1", "store-1", 2, 1))
        );

        SubmitOrderResult result = submitOrderSagaService.submit(command);

        assertThat(result.orderIds()).hasSize(1);
        assertThat(savedOrders).singleElement().satisfies(order -> {
            assertThat(order.getPayableAmount()).isEqualTo(3998L);
            assertThat(order.getPricingSnapshotRef()).isEqualTo("pricing-snapshot:biz-1:seller-1");
            assertThat(order.getInventoryReservationRef()).isEqualTo("inventory-reservation:biz-1:seller-1");
        });
    }

    @Test
    void submitRejectsWhenQuoteFails() {
        when(pricingInventoryGateway.reserveInventory(eq("biz-quote-fail:seller-1"), any()))
                .thenThrow(new OrderRejectedException(
                        "biz-quote-fail",
                        "PRICE_MISSING",
                        "PRICE_MISSING: sku-404"
                ));

        SubmitOrderCommand command = new SubmitOrderCommand(
                "biz-quote-fail",
                "buyer-1",
                "zh-CN",
                List.of(new SubmitOrderItem("MERCHANT_OFFER_SKU", "sku-404", "seller-1", "store-1", 1, 0))
        );

        assertThatThrownBy(() -> submitOrderSagaService.submit(command))
                .isInstanceOf(OrderRejectedException.class)
                .hasMessageContaining("PRICE_MISSING");

        assertThat(savedOrders).isEmpty();
        assertThat(savedSubOrders).isEmpty();
    }

    @Test
    void submitRejectsWhenReservationFails() {
        when(pricingInventoryGateway.reserveInventory(eq("biz-reserve-fail:seller-1"), any()))
                .thenThrow(new OrderRejectedException(
                        "biz-reserve-fail",
                        "INVENTORY_INSUFFICIENT",
                        "INVENTORY_INSUFFICIENT: sku-1"
                ));

        SubmitOrderCommand command = new SubmitOrderCommand(
                "biz-reserve-fail",
                "buyer-1",
                "zh-CN",
                List.of(new SubmitOrderItem("MERCHANT_OFFER_SKU", "sku-1", "seller-1", "store-1", 1, 0))
        );

        assertThatThrownBy(() -> submitOrderSagaService.submit(command))
                .isInstanceOf(OrderRejectedException.class)
                .hasMessageContaining("INVENTORY_INSUFFICIENT");

        assertThat(savedOrders).isEmpty();
        assertThat(savedSubOrders).isEmpty();
    }

    @Test
    void submitReleasesReservationWhenOrderPersistenceFails() {
        when(pricingInventoryGateway.reserveInventory(eq("biz-order-save-fail:seller-1"), any()))
                .thenReturn(new PricingInventoryGateway.InventoryReservationResult(
                        "inventory-reservation:biz-order-save-fail:seller-1",
                        "pricing-snapshot:biz-order-save-fail:seller-1",
                        1999L,
                        "SELLABLE",
                        List.of(new PricingInventoryGateway.QuotedOrderLine(
                                "MERCHANT_OFFER_SKU",
                                "sku-1",
                                "source-sku-1",
                                1500L,
                                null,
                                1999L,
                                1999L,
                                1999L,
                                "SELLABLE",
                                null
                        ))
                ));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenThrow(new IllegalStateException("order save failed"));

        SubmitOrderCommand command = new SubmitOrderCommand(
                "biz-order-save-fail",
                "buyer-1",
                "zh-CN",
                List.of(new SubmitOrderItem("MERCHANT_OFFER_SKU", "sku-1", "seller-1", "store-1", 1, 0))
        );

        assertThatThrownBy(() -> submitOrderSagaService.submit(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("order save failed");

        verify(pricingInventoryGateway).releaseInventory(
                "inventory-reservation:biz-order-save-fail:seller-1",
                "release:biz-order-save-fail:seller-1"
        );
        assertThat(savedOrders).isEmpty();
        assertThat(savedOutboxEvents).isEmpty();
    }

    @Test
    void submitAppendsOrderRejectedEventAndThrowsWhenEligibilityIsMissing() throws Exception {
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("seller-404", "store-404"))
                .thenReturn(Optional.empty());

        SubmitOrderCommand command = new SubmitOrderCommand(
                "biz-rejected",
                "buyer-404",
                "zh-CN",
                List.of(new SubmitOrderItem("MERCHANT_OFFER_SKU", "sku-404", "seller-404", "store-404", 1, 1999))
        );

        assertThatThrownBy(() -> submitOrderSagaService.submit(command))
                .isInstanceOf(OrderRejectedException.class)
                .hasMessage("未找到可用经营资格: seller-404/store-404");

        assertThat(savedOrders).isEmpty();
        assertThat(savedSubOrders).isEmpty();
        assertThat(savedOutboxEvents).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo("OrderRejected");
            JsonNode payload = new ObjectMapper().readTree(event.getPayload());
            assertThat(payload.get("eventType").asText()).isEqualTo("OrderRejected");
            assertThat(payload.get("aggregateId").asText()).isEqualTo("biz-rejected");
            assertThat(payload.get("payload").get("reasonCode").asText()).isEqualTo("ELIGIBILITY_MISSING");
            assertThat(payload.get("payload").get("businessIdempotencyKey").asText()).isEqualTo("biz-rejected");
        });
    }

    @Test
    void submitDoesNotAppendDuplicateRejectedEventForSameBusinessIdempotencyKey() {
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("seller-404", "store-404"))
                .thenReturn(Optional.empty());

        SubmitOrderCommand command = new SubmitOrderCommand(
                "biz-rejected",
                "buyer-404",
                "zh-CN",
                List.of(new SubmitOrderItem("MERCHANT_OFFER_SKU", "sku-404", "seller-404", "store-404", 1, 1999))
        );

        assertThatThrownBy(() -> submitOrderSagaService.submit(command))
                .isInstanceOf(OrderRejectedException.class);
        assertThatThrownBy(() -> submitOrderSagaService.submit(command))
                .isInstanceOf(OrderRejectedException.class);

        assertThat(savedOutboxEvents).hasSize(1);
        assertThat(savedOrders).isEmpty();
    }

    @Test
    void confirmOrderInventoryCallsPricingInventoryGateway() {
        OrderEntity order = new OrderEntity(
                "order-1",
                "ORD-biz-1-1",
                "biz-1",
                "buyer-1",
                "seller-1",
                "MERCHANT",
                "seller-1",
                "org-1",
                "MERCHANT_SELF",
                "store-1",
                "CNY",
                3998L,
                0L,
                0L,
                3998L,
                "{\"receiver\":\"pending\"}",
                "pricing-snapshot:biz-1:seller-1",
                "inventory-reservation:biz-1:seller-1",
                "SUBMITTED",
                OffsetDateTime.now()
        );
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(pricingInventoryGateway.confirmInventory(
                "inventory-reservation:biz-1:seller-1",
                "confirm:order-1"
        )).thenReturn(new PricingInventoryGateway.InventoryReservationResult(
                "inventory-reservation:biz-1:seller-1",
                null,
                0L,
                "SELLABLE",
                List.of()
        ));

        PricingInventoryGateway.InventoryReservationResult result = submitOrderSagaService.confirmOrderInventory("order-1");

        assertThat(result.inventoryReservationRef()).isEqualTo("inventory-reservation:biz-1:seller-1");
        assertThat(result.sellableState()).isEqualTo("SELLABLE");
    }

    @Test
    void submitSplitsDirectAndMerchantSupplyOrdersBySeller() {
        when(eligibilityProjectionRepository.findBySellerIdAndStorefrontId("direct-seller-1", "store-direct-1"))
                .thenReturn(Optional.of(new EligibilityProjectionEntity(
                        "direct-seller-1",
                        "store-direct-1",
                        "org-direct-1",
                        true,
                        OffsetDateTime.now()
                )));
        when(pricingInventoryGateway.reserveInventory(eq("biz-mixed:seller-1"), any()))
                .thenReturn(new PricingInventoryGateway.InventoryReservationResult(
                        "inventory-reservation:biz-mixed:seller-1",
                        "pricing-snapshot:biz-mixed:seller-1",
                        1999L,
                        "SELLABLE",
                        List.of()
                ));
        when(pricingInventoryGateway.reserveInventory(eq("biz-mixed:direct-seller-1"), any()))
                .thenReturn(new PricingInventoryGateway.InventoryReservationResult(
                        "inventory-reservation:biz-mixed:direct-seller-1",
                        "pricing-snapshot:biz-mixed:direct-seller-1",
                        2999L,
                        "SELLABLE",
                        List.of()
                ));

        SubmitOrderResult result = submitOrderSagaService.submit(new SubmitOrderCommand(
                "biz-mixed",
                "buyer-1",
                "zh-CN",
                List.of(
                        new SubmitOrderItem("MERCHANT_OFFER_SKU", "sku-1", "seller-1", "store-1", 1, 1999),
                        new SubmitOrderItem("SOURCE_SKU", "source-sku-1", "direct-seller-1", "store-direct-1", 1, 2999)
                )
        ));

        assertThat(result.orderIds()).hasSize(2);
        assertThat(savedOrders).extracting(OrderEntity::getOrderScene)
                .containsExactly("MERCHANT_SUPPLY", "DIRECT_SUPPLIER");
    }
}
