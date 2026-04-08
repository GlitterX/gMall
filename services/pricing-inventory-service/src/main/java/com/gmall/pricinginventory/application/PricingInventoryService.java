package com.gmall.pricinginventory.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.pricinginventory.infrastructure.persistence.DirectRetailPriceEntity;
import com.gmall.pricinginventory.infrastructure.persistence.DirectRetailPriceRepository;
import com.gmall.pricinginventory.infrastructure.persistence.InventoryBalanceEntity;
import com.gmall.pricinginventory.infrastructure.persistence.InventoryBalanceRepository;
import com.gmall.pricinginventory.infrastructure.persistence.InventoryLedgerEntryEntity;
import com.gmall.pricinginventory.infrastructure.persistence.InventoryLedgerEntryRepository;
import com.gmall.pricinginventory.infrastructure.persistence.InventoryReservationEntity;
import com.gmall.pricinginventory.infrastructure.persistence.InventoryReservationRepository;
import com.gmall.pricinginventory.infrastructure.persistence.MerchantOfferPriceEntity;
import com.gmall.pricinginventory.infrastructure.persistence.MerchantOfferPriceRepository;
import com.gmall.pricinginventory.infrastructure.persistence.PricingSnapshotEntity;
import com.gmall.pricinginventory.infrastructure.persistence.PricingSnapshotRepository;
import com.gmall.pricinginventory.infrastructure.persistence.SellableProjectionEntity;
import com.gmall.pricinginventory.infrastructure.persistence.SellableProjectionRepository;
import com.gmall.pricinginventory.infrastructure.persistence.SourceSupplyPriceEntity;
import com.gmall.pricinginventory.infrastructure.persistence.SourceSupplyPriceRepository;
import com.gmall.pricinginventory.infrastructure.remote.PricingInventoryRemoteException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingInventoryService {

    private final CatalogSkuMappingGateway catalogSkuMappingGateway;
    private final OrganizationContextGateway organizationContextGateway;
    private final SupplyRelationGateway supplyRelationGateway;
    private final MerchantOfferPriceRepository merchantOfferPriceRepository;
    private final SourceSupplyPriceRepository sourceSupplyPriceRepository;
    private final DirectRetailPriceRepository directRetailPriceRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryLedgerEntryRepository inventoryLedgerEntryRepository;
    private final PricingSnapshotRepository pricingSnapshotRepository;
    private final SellableProjectionRepository sellableProjectionRepository;
    private final ObjectMapper objectMapper;

    public PricingInventoryService(CatalogSkuMappingGateway catalogSkuMappingGateway,
                                   OrganizationContextGateway organizationContextGateway,
                                   SupplyRelationGateway supplyRelationGateway,
                                   MerchantOfferPriceRepository merchantOfferPriceRepository,
                                   SourceSupplyPriceRepository sourceSupplyPriceRepository,
                                   DirectRetailPriceRepository directRetailPriceRepository,
                                   InventoryBalanceRepository inventoryBalanceRepository,
                                   InventoryReservationRepository inventoryReservationRepository,
                                   InventoryLedgerEntryRepository inventoryLedgerEntryRepository,
                                   PricingSnapshotRepository pricingSnapshotRepository,
                                   SellableProjectionRepository sellableProjectionRepository,
                                   ObjectMapper objectMapper) {
        this.catalogSkuMappingGateway = catalogSkuMappingGateway;
        this.organizationContextGateway = organizationContextGateway;
        this.supplyRelationGateway = supplyRelationGateway;
        this.merchantOfferPriceRepository = merchantOfferPriceRepository;
        this.sourceSupplyPriceRepository = sourceSupplyPriceRepository;
        this.directRetailPriceRepository = directRetailPriceRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.inventoryLedgerEntryRepository = inventoryLedgerEntryRepository;
        this.pricingSnapshotRepository = pricingSnapshotRepository;
        this.sellableProjectionRepository = sellableProjectionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PricingInventoryModels.PriceView maintainSupplyPrice(String sourceSkuId, PricingInventoryModels.PriceCommand command) {
        validatePriceCommand(command);
        SourceSupplyPriceEntity entity = new SourceSupplyPriceEntity(
                priceId("SUPPLY", command.idempotencyKey(), sourceSkuId),
                sourceSkuId,
                command.amount(),
                command.currencyCode(),
                "ACTIVE",
                command.effectiveAt(),
                OffsetDateTime.now()
        );
        sourceSupplyPriceRepository.save(entity);
        return new PricingInventoryModels.PriceView(
                "SUPPLY",
                "SOURCE_SKU",
                sourceSkuId,
                entity.getAmount(),
                entity.getCurrencyCode(),
                entity.getStatus(),
                entity.getEffectiveAt()
        );
    }

    @Transactional
    public PricingInventoryModels.PriceView maintainDirectRetailPrice(String sourceSkuId, PricingInventoryModels.PriceCommand command) {
        validatePriceCommand(command);
        CatalogSkuMappingGateway.BusinessSkuMapping mapping = getMapping("SOURCE_SKU", sourceSkuId);
        OrganizationContextGateway.OrganizationContext context = getOrganizationContext(mapping.ownerId());
        if (!context.directSupplierQualified()) {
            throw reject("DIRECT_QUALIFICATION_REQUIRED", "DIRECT_QUALIFICATION_REQUIRED: " + sourceSkuId);
        }
        DirectRetailPriceEntity entity = new DirectRetailPriceEntity(
                priceId("DIRECT", command.idempotencyKey(), sourceSkuId),
                sourceSkuId,
                command.amount(),
                command.currencyCode(),
                "ACTIVE",
                command.effectiveAt(),
                OffsetDateTime.now()
        );
        directRetailPriceRepository.save(entity);
        rebuildSellableProjection(mapping);
        return new PricingInventoryModels.PriceView(
                "DIRECT",
                "SOURCE_SKU",
                sourceSkuId,
                entity.getAmount(),
                entity.getCurrencyCode(),
                entity.getStatus(),
                entity.getEffectiveAt()
        );
    }

    @Transactional
    public PricingInventoryModels.PriceView maintainMerchantOfferPrice(String merchantOfferSkuId, PricingInventoryModels.PriceCommand command) {
        validatePriceCommand(command);
        CatalogSkuMappingGateway.BusinessSkuMapping mapping = getMapping("MERCHANT_OFFER_SKU", merchantOfferSkuId);
        SourceSupplyPriceEntity supplyPrice = sourceSupplyPriceRepository.findActiveBySourceSkuId(mapping.sourceSkuId())
                .orElseThrow(() -> reject("SUPPLY_PRICE_MISSING", "SUPPLY_PRICE_MISSING: " + mapping.sourceSkuId()));
        if (command.amount() < supplyPrice.getAmount()) {
            throw reject("PRICE_BELOW_SUPPLY", "PRICE_BELOW_SUPPLY: " + merchantOfferSkuId);
        }
        MerchantOfferPriceEntity entity = new MerchantOfferPriceEntity(
                priceId("MERCHANT", command.idempotencyKey(), merchantOfferSkuId),
                merchantOfferSkuId,
                mapping.sourceSkuId(),
                command.amount(),
                command.currencyCode(),
                "ACTIVE",
                command.effectiveAt(),
                OffsetDateTime.now()
        );
        merchantOfferPriceRepository.save(entity);
        rebuildSellableProjection(mapping);
        return new PricingInventoryModels.PriceView(
                "MERCHANT",
                "MERCHANT_OFFER_SKU",
                merchantOfferSkuId,
                entity.getAmount(),
                entity.getCurrencyCode(),
                entity.getStatus(),
                entity.getEffectiveAt()
        );
    }

    @Transactional
    public InventoryBalanceEntity adjustInventory(String sourceSkuId, PricingInventoryModels.InventoryAdjustmentCommand command) {
        if (command == null || command.businessKey() == null || command.businessKey().isBlank()) {
            throw new IllegalArgumentException("库存调整业务键不能为空");
        }
        String ledgerId = ledgerId(command.businessKey(), sourceSkuId, "ADJUST");
        if (inventoryLedgerEntryRepository.findById(ledgerId).isPresent()) {
            return inventoryBalanceRepository.findById(sourceSkuId)
                    .orElseThrow(() -> new IllegalArgumentException("库存不存在: " + sourceSkuId));
        }
        InventoryBalanceEntity balance = inventoryBalanceRepository.findById(sourceSkuId)
                .orElse(new InventoryBalanceEntity(sourceSkuId, 0, 0, 0, 0L));
        balance.adjust(command.deltaQuantity());
        inventoryBalanceRepository.save(balance);
        inventoryLedgerEntryRepository.save(new InventoryLedgerEntryEntity(
                ledgerId,
                sourceSkuId,
                command.businessKey(),
                "ADJUST",
                command.deltaQuantity(),
                OffsetDateTime.now()
        ));
        return balance;
    }

    @Transactional(readOnly = true)
    public PricingInventoryModels.QuoteResult quote(PricingInventoryModels.QuoteCommand command) {
        validateOrderLines(command == null ? null : command.items());
        EvaluationResult evaluation = evaluate(command.items(), false);
        return new PricingInventoryModels.QuoteResult(evaluation.totalAmount(), evaluation.items());
    }

    @Transactional
    public PricingInventoryModels.ReservationResult reserve(PricingInventoryModels.ReservationCommand command) {
        validateOrderLines(command == null ? null : command.items());
        Optional<InventoryReservationEntity> existingReservation = inventoryReservationRepository.findByBusinessKey(command.businessKey());
        Optional<PricingSnapshotEntity> existingSnapshot = pricingSnapshotRepository.findByBusinessKey(command.businessKey());
        if (existingReservation.isPresent() && existingSnapshot.isPresent()) {
            PricingInventoryModels.PricingSnapshotView snapshot = getSnapshot(command.businessKey());
            return new PricingInventoryModels.ReservationResult(
                    existingReservation.get().getReservationId(),
                    snapshot.pricingSnapshotRef(),
                    snapshot.totalAmount(),
                    "SELLABLE",
                    snapshot.items()
            );
        }

        EvaluationResult evaluation = evaluate(command.items(), true);
        String reservationRef = "inventory-reservation:" + command.businessKey();
        inventoryReservationRepository.save(new InventoryReservationEntity(
                reservationRef,
                command.businessKey(),
                "ACTIVE",
                writeReservationPayload(evaluation.resolvedItems()),
                OffsetDateTime.now()
        ));
        String snapshotRef = "pricing-snapshot:" + command.businessKey();
        pricingSnapshotRepository.save(new PricingSnapshotEntity(
                snapshotRef,
                command.businessKey(),
                evaluation.totalAmount(),
                writeQuotedItemsPayload(evaluation.items()),
                OffsetDateTime.now()
        ));
        return new PricingInventoryModels.ReservationResult(
                reservationRef,
                snapshotRef,
                evaluation.totalAmount(),
                "SELLABLE",
                evaluation.items()
        );
    }

    @Transactional
    public PricingInventoryModels.InventoryReservationResult releaseInventory(
            String reservationId,
            PricingInventoryModels.InventoryReservationActionCommand command
    ) {
        InventoryReservationEntity reservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("库存预占不存在: " + reservationId));
        if ("RELEASED".equals(reservation.getStatus())) {
            return new PricingInventoryModels.InventoryReservationResult(reservationId, "NOT_SELLABLE");
        }
        if ("CONFIRMED".equals(reservation.getStatus())) {
            throw reject("RESERVATION_ALREADY_CONFIRMED", "RESERVATION_ALREADY_CONFIRMED: " + reservationId);
        }
        for (ResolvedReservationItem item : parseReservationItems(reservation.getReservationPayload())) {
            InventoryBalanceEntity balance = inventoryBalanceRepository.findById(item.sourceSkuId())
                    .orElseThrow(() -> reject("INVENTORY_MISSING", "INVENTORY_MISSING: " + item.sourceSkuId()));
            balance.release(item.quantity());
            inventoryBalanceRepository.save(balance);
            inventoryLedgerEntryRepository.save(new InventoryLedgerEntryEntity(
                    ledgerId(command.businessKey(), item.sourceSkuId(), "RELEASE"),
                    item.sourceSkuId(),
                    command.businessKey(),
                    "RELEASE",
                    item.quantity(),
                    OffsetDateTime.now()
            ));
            rebuildSellableProjection(getMapping(item.businessSkuType(), item.businessSkuId()));
        }
        reservation.markReleased();
        inventoryReservationRepository.save(reservation);
        return new PricingInventoryModels.InventoryReservationResult(reservationId, "NOT_SELLABLE");
    }

    @Transactional
    public PricingInventoryModels.InventoryReservationResult confirmInventory(
            String reservationId,
            PricingInventoryModels.InventoryReservationActionCommand command
    ) {
        InventoryReservationEntity reservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("库存预占不存在: " + reservationId));
        if ("CONFIRMED".equals(reservation.getStatus())) {
            return new PricingInventoryModels.InventoryReservationResult(reservationId, "SELLABLE");
        }
        if ("RELEASED".equals(reservation.getStatus())) {
            throw reject("RESERVATION_ALREADY_RELEASED", "RESERVATION_ALREADY_RELEASED: " + reservationId);
        }
        String sellableState = "NOT_SELLABLE";
        for (ResolvedReservationItem item : parseReservationItems(reservation.getReservationPayload())) {
            InventoryBalanceEntity balance = inventoryBalanceRepository.findById(item.sourceSkuId())
                    .orElseThrow(() -> reject("INVENTORY_MISSING", "INVENTORY_MISSING: " + item.sourceSkuId()));
            balance.confirm(item.quantity());
            inventoryBalanceRepository.save(balance);
            inventoryLedgerEntryRepository.save(new InventoryLedgerEntryEntity(
                    ledgerId(command.businessKey(), item.sourceSkuId(), "CONFIRM"),
                    item.sourceSkuId(),
                    command.businessKey(),
                    "CONFIRM",
                    item.quantity(),
                    OffsetDateTime.now()
            ));
            PricingInventoryModels.SellableView sellable = rebuildSellableProjection(getMapping(item.businessSkuType(), item.businessSkuId()));
            if ("SELLABLE".equals(sellable.sellableState())) {
                sellableState = "SELLABLE";
            }
        }
        reservation.markConfirmed();
        inventoryReservationRepository.save(reservation);
        return new PricingInventoryModels.InventoryReservationResult(reservationId, sellableState);
    }

    @Transactional(readOnly = true)
    public PricingInventoryModels.PricingSnapshotView getSnapshot(String businessKey) {
        PricingSnapshotEntity snapshot = pricingSnapshotRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new IllegalArgumentException("价格快照不存在: " + businessKey));
        return new PricingInventoryModels.PricingSnapshotView(
                snapshot.getPricingSnapshotRef(),
                snapshot.getBusinessKey(),
                snapshot.getTotalAmount(),
                readQuotedItems(snapshot.getSnapshotPayload())
        );
    }

    @Transactional(readOnly = true)
    public PricingInventoryModels.SellableView getSellable(String businessSkuType, String businessSkuId) {
        return sellableProjectionRepository.findByBusinessSkuTypeAndBusinessSkuId(businessSkuType, businessSkuId)
                .map(projection -> new PricingInventoryModels.SellableView(
                        projection.getBusinessSkuType(),
                        projection.getBusinessSkuId(),
                        projection.getSourceSkuId(),
                        projection.getSellableState(),
                        projection.getReasonCode()
                ))
                .orElseGet(() -> {
                    CatalogSkuMappingGateway.BusinessSkuMapping mapping = getMapping(businessSkuType, businessSkuId);
                    return evaluateSellable(mapping);
                });
    }

    private EvaluationResult evaluate(List<PricingInventoryModels.BusinessOrderLine> items, boolean reserveInventory) {
        List<PricingInventoryModels.QuotedOrderLine> quotedItems = new ArrayList<>();
        List<ResolvedReservationItem> resolvedItems = new ArrayList<>();
        Map<String, Integer> reserveQtyBySourceSku = new LinkedHashMap<>();
        long totalAmount = 0L;
        for (PricingInventoryModels.BusinessOrderLine item : items) {
            CatalogSkuMappingGateway.BusinessSkuMapping mapping = getMapping(item.businessSkuType(), item.businessSkuId());
            EvaluationLine evaluationLine = evaluateLine(mapping, item.quantity());
            if (!"SELLABLE".equals(evaluationLine.sellableView().sellableState())) {
                throw reject(evaluationLine.sellableView().reasonCode(),
                        evaluationLine.sellableView().reasonCode() + ": " + item.businessSkuType() + ":" + item.businessSkuId());
            }
            long subtotal = evaluationLine.unitPrice() * item.quantity();
            totalAmount += subtotal;
            quotedItems.add(new PricingInventoryModels.QuotedOrderLine(
                    item.businessSkuType(),
                    item.businessSkuId(),
                    mapping.sourceSkuId(),
                    evaluationLine.supplyPrice(),
                    evaluationLine.directRetailPrice(),
                    evaluationLine.merchantRetailPrice(),
                    evaluationLine.unitPrice(),
                    subtotal,
                    evaluationLine.sellableView().sellableState(),
                    evaluationLine.sellableView().reasonCode()
            ));
            reserveQtyBySourceSku.merge(mapping.sourceSkuId(), item.quantity(), Integer::sum);
            resolvedItems.add(new ResolvedReservationItem(
                    item.businessSkuType(),
                    item.businessSkuId(),
                    mapping.sourceSkuId(),
                    item.quantity()
            ));
        }
        if (reserveInventory) {
            for (Map.Entry<String, Integer> entry : reserveQtyBySourceSku.entrySet()) {
                InventoryBalanceEntity balance = inventoryBalanceRepository.findById(entry.getKey())
                        .orElseThrow(() -> reject("INVENTORY_MISSING", "INVENTORY_MISSING: " + entry.getKey()));
                if (balance.getAvailableQty() < entry.getValue()) {
                    throw reject("INVENTORY_INSUFFICIENT", "INVENTORY_INSUFFICIENT: " + entry.getKey());
                }
                balance.reserve(entry.getValue());
                inventoryBalanceRepository.save(balance);
                inventoryLedgerEntryRepository.save(new InventoryLedgerEntryEntity(
                        ledgerId("reserve", entry.getKey(), UUID.randomUUID().toString()),
                        entry.getKey(),
                        "reserve",
                        "RESERVE",
                        -entry.getValue(),
                        OffsetDateTime.now()
                ));
            }
        }
        return new EvaluationResult(totalAmount, quotedItems, resolvedItems);
    }

    private EvaluationLine evaluateLine(CatalogSkuMappingGateway.BusinessSkuMapping mapping, int quantity) {
        PricingInventoryModels.SellableView sellableView = evaluateSellable(mapping);
        if (!"SELLABLE".equals(sellableView.sellableState())) {
            return new EvaluationLine(null, null, null, 0L, sellableView);
        }
        if ("MERCHANT_OFFER_SKU".equals(mapping.businessSkuType())) {
            SourceSupplyPriceEntity supplyPrice = sourceSupplyPriceRepository.findActiveBySourceSkuId(mapping.sourceSkuId())
                    .orElseThrow(() -> reject("SUPPLY_PRICE_MISSING", "SUPPLY_PRICE_MISSING: " + mapping.sourceSkuId()));
            MerchantOfferPriceEntity merchantOfferPrice = merchantOfferPriceRepository.findActiveByMerchantOfferSkuId(mapping.businessSkuId())
                    .orElseThrow(() -> reject("PRICE_MISSING", "PRICE_MISSING: " + mapping.businessSkuId()));
            return new EvaluationLine(
                    supplyPrice.getAmount(),
                    null,
                    merchantOfferPrice.getAmount(),
                    merchantOfferPrice.getAmount(),
                    sellableView
            );
        }
        DirectRetailPriceEntity directRetailPrice = directRetailPriceRepository.findActiveBySourceSkuId(mapping.sourceSkuId())
                .orElseThrow(() -> reject("DIRECT_PRICE_MISSING", "DIRECT_PRICE_MISSING: " + mapping.sourceSkuId()));
        return new EvaluationLine(
                null,
                directRetailPrice.getAmount(),
                null,
                directRetailPrice.getAmount(),
                sellableView
        );
    }

    private PricingInventoryModels.SellableView evaluateSellable(CatalogSkuMappingGateway.BusinessSkuMapping mapping) {
        String reasonCode = "OK";
        String sellableState = "SELLABLE";
        OrganizationContextGateway.OrganizationContext context = getOrganizationContext(mapping.ownerId());
        if (!"ACTIVE".equals(context.status())) {
            reasonCode = "ORGANIZATION_INACTIVE";
            sellableState = "NOT_SELLABLE";
        } else if (!isProductStatusSellable(mapping.sourceProductStatus()) || !isSkuStatusSellable(mapping.offerSkuStatus())) {
            reasonCode = "CATALOG_PRODUCT_INACTIVE";
            sellableState = "NOT_SELLABLE";
        } else if ("MERCHANT_OFFER_SKU".equals(mapping.businessSkuType())) {
            if (!isProductStatusSellable(mapping.offerProductStatus())) {
                reasonCode = "OFFER_PRODUCT_INACTIVE";
                sellableState = "NOT_SELLABLE";
            } else {
                SupplyRelationGateway.SupplyRelationContext relationContext = supplyRelationGateway.resolveSupplyRelation(
                        mapping.ownerId(),
                        mapping.merchantId(),
                        mapping.sourceProductId()
                );
                if (!relationContext.active() || !relationContext.catalogAuthorized()) {
                    reasonCode = relationContext.authorizationReason();
                    sellableState = "NOT_SELLABLE";
                } else {
                    SourceSupplyPriceEntity supplyPrice = sourceSupplyPriceRepository.findActiveBySourceSkuId(mapping.sourceSkuId()).orElse(null);
                    MerchantOfferPriceEntity merchantPrice = merchantOfferPriceRepository.findActiveByMerchantOfferSkuId(mapping.businessSkuId()).orElse(null);
                    if (supplyPrice == null || merchantPrice == null) {
                        reasonCode = supplyPrice == null ? "SUPPLY_PRICE_MISSING" : "PRICE_MISSING";
                        sellableState = "NOT_SELLABLE";
                    } else if (merchantPrice.getAmount() < supplyPrice.getAmount()) {
                        reasonCode = "PRICE_BELOW_SUPPLY";
                        sellableState = "NOT_SELLABLE";
                    }
                }
            }
        } else {
            if (!context.directSupplierQualified()) {
                reasonCode = "DIRECT_QUALIFICATION_REQUIRED";
                sellableState = "NOT_SELLABLE";
            } else if (directRetailPriceRepository.findActiveBySourceSkuId(mapping.sourceSkuId()).isEmpty()) {
                reasonCode = "DIRECT_PRICE_MISSING";
                sellableState = "NOT_SELLABLE";
            }
        }
        InventoryBalanceEntity balance = inventoryBalanceRepository.findById(mapping.sourceSkuId()).orElse(null);
        if ("SELLABLE".equals(sellableState) && (balance == null || balance.getAvailableQty() <= 0)) {
            reasonCode = balance == null ? "INVENTORY_MISSING" : "INVENTORY_INSUFFICIENT";
            sellableState = "NOT_SELLABLE";
        }
        SellableProjectionEntity projection = new SellableProjectionEntity(
                sellableId(mapping.businessSkuType(), mapping.businessSkuId()),
                mapping.businessSkuType(),
                mapping.businessSkuId(),
                mapping.sourceSkuId(),
                sellableState,
                reasonCode,
                OffsetDateTime.now()
        );
        sellableProjectionRepository.save(projection);
        return new PricingInventoryModels.SellableView(
                mapping.businessSkuType(),
                mapping.businessSkuId(),
                mapping.sourceSkuId(),
                sellableState,
                reasonCode
        );
    }

    private PricingInventoryModels.SellableView rebuildSellableProjection(CatalogSkuMappingGateway.BusinessSkuMapping mapping) {
        return evaluateSellable(mapping);
    }

    private CatalogSkuMappingGateway.BusinessSkuMapping getMapping(String businessSkuType, String businessSkuId) {
        try {
            return catalogSkuMappingGateway.getSkuMapping(businessSkuType, businessSkuId);
        } catch (PricingInventoryRemoteException exception) {
            throw reject(exception.reasonCode(), exception.getMessage());
        }
    }

    private OrganizationContextGateway.OrganizationContext getOrganizationContext(String organizationId) {
        try {
            return organizationContextGateway.getOrganizationContext(organizationId);
        } catch (PricingInventoryRemoteException exception) {
            throw reject(exception.reasonCode(), exception.getMessage());
        }
    }

    private void validatePriceCommand(PricingInventoryModels.PriceCommand command) {
        if (command == null || command.amount() <= 0 || command.currencyCode() == null || command.currencyCode().isBlank()
                || command.idempotencyKey() == null || command.idempotencyKey().isBlank() || command.effectiveAt() == null) {
            throw new IllegalArgumentException("价格维护入参不完整");
        }
    }

    private void validateOrderLines(List<PricingInventoryModels.BusinessOrderLine> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("订单行不能为空");
        }
        for (PricingInventoryModels.BusinessOrderLine item : items) {
            if (item == null
                    || item.businessSkuType() == null || item.businessSkuType().isBlank()
                    || item.businessSkuId() == null || item.businessSkuId().isBlank()) {
                throw new IllegalArgumentException("businessSku 不能为空");
            }
            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("quantity 必须大于 0");
            }
        }
    }

    private boolean isProductStatusSellable(String status) {
        return status != null && ("ACTIVE".equals(status) || "ON_SHELF".equals(status));
    }

    private boolean isSkuStatusSellable(String status) {
        return status == null || "ACTIVE".equals(status) || "ON_SHELF".equals(status);
    }

    private String writeReservationPayload(List<ResolvedReservationItem> items) {
        try {
            return objectMapper.writeValueAsString(Map.of("items", items));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化库存预占明细", exception);
        }
    }

    private List<ResolvedReservationItem> parseReservationItems(String payload) {
        try {
            Map<String, List<ResolvedReservationItem>> wrapper = objectMapper.readValue(payload, new TypeReference<>() {
            });
            return wrapper.getOrDefault("items", List.of());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析库存预占明细", exception);
        }
    }

    private String writeQuotedItemsPayload(List<PricingInventoryModels.QuotedOrderLine> items) {
        try {
            return objectMapper.writeValueAsString(Map.of("items", items));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化价格快照明细", exception);
        }
    }

    private List<PricingInventoryModels.QuotedOrderLine> readQuotedItems(String payload) {
        try {
            Map<String, List<PricingInventoryModels.QuotedOrderLine>> wrapper = objectMapper.readValue(payload, new TypeReference<>() {
            });
            return wrapper.getOrDefault("items", List.of());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析价格快照明细", exception);
        }
    }

    private String priceId(String prefix, String idempotencyKey, String businessSkuId) {
        return prefix + ":" + idempotencyKey + ":" + businessSkuId;
    }

    private String sellableId(String businessSkuType, String businessSkuId) {
        return businessSkuType + ":" + businessSkuId;
    }

    private String ledgerId(String businessKey, String sourceSkuId, String action) {
        return "ledger:" + action + ":" + businessKey + ":" + sourceSkuId;
    }

    private PricingInventoryRejectedException reject(String reasonCode, String detail) {
        return new PricingInventoryRejectedException(reasonCode, detail);
    }

    private record EvaluationLine(Long supplyPrice,
                                  Long directRetailPrice,
                                  Long merchantRetailPrice,
                                  long unitPrice,
                                  PricingInventoryModels.SellableView sellableView) {
    }

    private record EvaluationResult(long totalAmount,
                                    List<PricingInventoryModels.QuotedOrderLine> items,
                                    List<ResolvedReservationItem> resolvedItems) {
    }

    private record ResolvedReservationItem(String businessSkuType,
                                           String businessSkuId,
                                           String sourceSkuId,
                                           int quantity) {
    }
}
