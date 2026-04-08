package com.gmall.trade.infrastructure.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.trade.application.OrderRejectedException;
import com.gmall.trade.application.PricingInventoryGateway;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class PricingInventoryRestClient implements PricingInventoryGateway {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public PricingInventoryRestClient(RestClient.Builder restClientBuilder,
                                      ObjectMapper objectMapper,
                                      @Value("${gmall.pricing-inventory-service.base-url}") String pricingInventoryBaseUrl) {
        this(restClientBuilder.baseUrl(pricingInventoryBaseUrl).build(), objectMapper);
    }

    PricingInventoryRestClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public QuoteResult quoteForOrder(String businessKey, List<OrderLine> items) {
        try {
            QuoteResponse response = restClient.post()
                    .uri("/internal/quotes")
                    .body(new QuoteRequest(businessKey, items))
                    .retrieve()
                    .body(QuoteResponse.class);
            if (response == null) {
                throw new IllegalStateException("价格库存报价结果为空");
            }
            return new QuoteResult(response.totalAmount(), response.items());
        } catch (HttpClientErrorException exception) {
            throw toRejectedException(businessKey, exception);
        }
    }

    @Override
    public InventoryReservationResult reserveInventory(String businessKey, List<OrderLine> items) {
        try {
            ReservationResponse response = restClient.post()
                    .uri("/internal/reservations")
                    .body(new QuoteRequest(businessKey, items))
                    .retrieve()
                    .body(ReservationResponse.class);
            if (response == null) {
                throw new IllegalStateException("库存预占结果为空");
            }
            return new InventoryReservationResult(
                    response.inventoryReservationRef(),
                    response.pricingSnapshotRef(),
                    response.totalAmount(),
                    response.sellableState(),
                    response.items() == null ? List.of() : response.items()
            );
        } catch (HttpClientErrorException exception) {
            throw toRejectedException(businessKey, exception);
        }
    }

    @Override
    public InventoryReservationResult releaseInventory(String inventoryReservationRef, String businessKey) {
        try {
            ReservationResponse response = restClient.post()
                    .uri("/internal/reservations/{reservationId}/release", inventoryReservationRef)
                    .body(new ReservationActionRequest(businessKey))
                    .retrieve()
                    .body(ReservationResponse.class);
            if (response == null) {
                throw new IllegalStateException("库存释放结果为空");
            }
            return new InventoryReservationResult(response.inventoryReservationRef(), null, 0L, response.sellableState(), List.of());
        } catch (HttpClientErrorException exception) {
            throw toRejectedException(businessKey, exception);
        }
    }

    @Override
    public InventoryReservationResult confirmInventory(String inventoryReservationRef, String businessKey) {
        try {
            ReservationResponse response = restClient.post()
                    .uri("/internal/reservations/{reservationId}/confirm", inventoryReservationRef)
                    .body(new ReservationActionRequest(businessKey))
                    .retrieve()
                    .body(ReservationResponse.class);
            if (response == null) {
                throw new IllegalStateException("库存确认结果为空");
            }
            return new InventoryReservationResult(response.inventoryReservationRef(), null, 0L, response.sellableState(), List.of());
        } catch (HttpClientErrorException exception) {
            throw toRejectedException(businessKey, exception);
        }
    }

    @Override
    public PricingSnapshotView getSnapshot(String businessKey) {
        try {
            SnapshotResponse response = restClient.get()
                    .uri("/internal/snapshots/{businessKey}", businessKey)
                    .retrieve()
                    .body(SnapshotResponse.class);
            if (response == null) {
                throw new IllegalStateException("价格快照结果为空");
            }
            return new PricingSnapshotView(
                    response.pricingSnapshotRef(),
                    response.businessKey(),
                    response.totalAmount(),
                    response.items()
            );
        } catch (HttpClientErrorException exception) {
            throw toRejectedException(businessKey, exception);
        }
    }

    private OrderRejectedException toRejectedException(String businessKey, HttpClientErrorException exception) {
        try {
            ErrorResponse error = objectMapper.readValue(exception.getResponseBodyAsString(), ErrorResponse.class);
            String reasonCode = error.reasonCode() == null || error.reasonCode().isBlank()
                    ? "PRICING_INVENTORY_REJECTED"
                    : error.reasonCode();
            String detail = error.detail() == null || error.detail().isBlank()
                    ? exception.getMessage()
                    : error.detail();
            return new OrderRejectedException(businessKey, reasonCode, detail);
        } catch (JsonProcessingException jsonProcessingException) {
            return new OrderRejectedException(businessKey, "PRICING_INVENTORY_REJECTED", exception.getMessage());
        }
    }

    private record QuoteRequest(String businessKey, List<OrderLine> items) {
    }

    private record QuoteResponse(String pricingSnapshotRef,
                                 long totalAmount,
                                 List<QuotedOrderLine> items) {
    }

    private record ReservationActionRequest(String businessKey) {
    }

    private record ReservationResponse(String inventoryReservationRef,
                                       String pricingSnapshotRef,
                                       long totalAmount,
                                       String sellableState,
                                       List<QuotedOrderLine> items) {
    }

    private record SnapshotResponse(String pricingSnapshotRef,
                                    String businessKey,
                                    long totalAmount,
                                    List<QuotedOrderLine> items) {
    }

    private record ErrorResponse(String reasonCode, String detail) {
    }
}
