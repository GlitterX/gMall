package com.gmall.pricinginventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingInventoryServiceTest {

    private final CatalogSkuMappingGateway catalogSkuMappingGateway = mock(CatalogSkuMappingGateway.class);
    private final OrganizationContextGateway organizationContextGateway = mock(OrganizationContextGateway.class);
    private final SupplyRelationGateway supplyRelationGateway = mock(SupplyRelationGateway.class);
    private final MerchantOfferPriceRepository merchantOfferPriceRepository = mock(MerchantOfferPriceRepository.class);
    private final SourceSupplyPriceRepository sourceSupplyPriceRepository = mock(SourceSupplyPriceRepository.class);
    private final DirectRetailPriceRepository directRetailPriceRepository = mock(DirectRetailPriceRepository.class);
    private final InventoryBalanceRepository inventoryBalanceRepository = mock(InventoryBalanceRepository.class);
    private final InventoryReservationRepository inventoryReservationRepository = mock(InventoryReservationRepository.class);
    private final InventoryLedgerEntryRepository inventoryLedgerEntryRepository = mock(InventoryLedgerEntryRepository.class);
    private final PricingSnapshotRepository pricingSnapshotRepository = mock(PricingSnapshotRepository.class);
    private final SellableProjectionRepository sellableProjectionRepository = mock(SellableProjectionRepository.class);

    private final List<SourceSupplyPriceEntity> savedSupplyPrices = new ArrayList<>();
    private final List<DirectRetailPriceEntity> savedDirectRetailPrices = new ArrayList<>();
    private final List<MerchantOfferPriceEntity> savedMerchantOfferPrices = new ArrayList<>();
    private final List<InventoryBalanceEntity> savedBalances = new ArrayList<>();
    private final List<InventoryReservationEntity> savedReservations = new ArrayList<>();
    private final List<InventoryLedgerEntryEntity> savedLedgerEntries = new ArrayList<>();
    private final List<PricingSnapshotEntity> savedSnapshots = new ArrayList<>();
    private final List<SellableProjectionEntity> savedSellableProjections = new ArrayList<>();

    private PricingInventoryService pricingInventoryService;

    @BeforeEach
    void setUp() {
        pricingInventoryService = new PricingInventoryService(
                catalogSkuMappingGateway,
                organizationContextGateway,
                supplyRelationGateway,
                merchantOfferPriceRepository,
                sourceSupplyPriceRepository,
                directRetailPriceRepository,
                inventoryBalanceRepository,
                inventoryReservationRepository,
                inventoryLedgerEntryRepository,
                pricingSnapshotRepository,
                sellableProjectionRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        when(sourceSupplyPriceRepository.save(any(SourceSupplyPriceEntity.class))).thenAnswer(invocation -> {
            SourceSupplyPriceEntity entity = invocation.getArgument(0);
            savedSupplyPrices.add(entity);
            return entity;
        });
        when(directRetailPriceRepository.save(any(DirectRetailPriceEntity.class))).thenAnswer(invocation -> {
            DirectRetailPriceEntity entity = invocation.getArgument(0);
            savedDirectRetailPrices.add(entity);
            return entity;
        });
        when(merchantOfferPriceRepository.save(any(MerchantOfferPriceEntity.class))).thenAnswer(invocation -> {
            MerchantOfferPriceEntity entity = invocation.getArgument(0);
            savedMerchantOfferPrices.add(entity);
            return entity;
        });
        when(inventoryBalanceRepository.save(any(InventoryBalanceEntity.class))).thenAnswer(invocation -> {
            InventoryBalanceEntity entity = invocation.getArgument(0);
            savedBalances.add(entity);
            return entity;
        });
        when(inventoryReservationRepository.save(any(InventoryReservationEntity.class))).thenAnswer(invocation -> {
            InventoryReservationEntity entity = invocation.getArgument(0);
            savedReservations.add(entity);
            return entity;
        });
        when(inventoryLedgerEntryRepository.save(any(InventoryLedgerEntryEntity.class))).thenAnswer(invocation -> {
            InventoryLedgerEntryEntity entity = invocation.getArgument(0);
            savedLedgerEntries.add(entity);
            return entity;
        });
        when(pricingSnapshotRepository.save(any(PricingSnapshotEntity.class))).thenAnswer(invocation -> {
            PricingSnapshotEntity entity = invocation.getArgument(0);
            savedSnapshots.add(entity);
            return entity;
        });
        when(sellableProjectionRepository.save(any(SellableProjectionEntity.class))).thenAnswer(invocation -> {
            SellableProjectionEntity entity = invocation.getArgument(0);
            savedSellableProjections.add(entity);
            return entity;
        });
    }

    @Test
    void maintainSupplyPriceCreatesActiveSupplyPrice() {
        PricingInventoryModels.PriceView result = pricingInventoryService.maintainSupplyPrice(
                "source-sku-1",
                new PricingInventoryModels.PriceCommand(1800L, "CNY", "idem-1", OffsetDateTime.parse("2026-04-08T18:00:00+08:00"))
        );

        assertThat(result.businessSkuType()).isEqualTo("SOURCE_SKU");
        assertThat(result.businessSkuId()).isEqualTo("source-sku-1");
        assertThat(result.priceType()).isEqualTo("SUPPLY");
        assertThat(result.amount()).isEqualTo(1800L);
        assertThat(savedSupplyPrices).singleElement().satisfies(price -> assertThat(price.getAmount()).isEqualTo(1800L));
    }

    @Test
    void maintainDirectRetailPriceRejectsWhenOrganizationIsNotQualified() {
        stubSourceSkuMapping("source-sku-direct-1", "org-direct-1", "DIRECT", "ON_SHELF", "ACTIVE");
        when(organizationContextGateway.getOrganizationContext("org-direct-1"))
                .thenReturn(new OrganizationContextGateway.OrganizationContext(
                        "org-direct-1",
                        "SUPPLIER",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US",
                        false,
                        "SUSPENDED"
                ));

        assertThatThrownBy(() -> pricingInventoryService.maintainDirectRetailPrice(
                "source-sku-direct-1",
                new PricingInventoryModels.PriceCommand(3200L, "CNY", "idem-2", OffsetDateTime.parse("2026-04-08T18:00:00+08:00"))
        ))
                .isInstanceOf(PricingInventoryRejectedException.class)
                .hasMessageContaining("DIRECT_QUALIFICATION_REQUIRED");
    }

    @Test
    void quoteReturnsMerchantOfferPriceWhenMerchantSkuIsSellable() {
        stubMerchantOfferMapping("offer-sku-1", "source-sku-1", "source-product-1", "org-supplier-1", "org-merchant-1");
        when(organizationContextGateway.getOrganizationContext("org-supplier-1"))
                .thenReturn(new OrganizationContextGateway.OrganizationContext(
                        "org-supplier-1",
                        "SUPPLIER",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US",
                        false,
                        "NOT_FOUND"
                ));
        when(supplyRelationGateway.resolveSupplyRelation("org-supplier-1", "org-merchant-1", "source-product-1"))
                .thenReturn(new SupplyRelationGateway.SupplyRelationContext(
                        "relation-1",
                        "ACTIVE",
                        true,
                        true,
                        "PRODUCT_AUTHORIZED"
                ));
        when(sourceSupplyPriceRepository.findActiveBySourceSkuId("source-sku-1"))
                .thenReturn(Optional.of(new SourceSupplyPriceEntity(
                        "supply-price-1",
                        "source-sku-1",
                        1800L,
                        "CNY",
                        "ACTIVE",
                        OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-08T18:00:00+08:00")
                )));
        when(merchantOfferPriceRepository.findActiveByMerchantOfferSkuId("offer-sku-1"))
                .thenReturn(Optional.of(new MerchantOfferPriceEntity(
                        "offer-price-1",
                        "offer-sku-1",
                        "source-sku-1",
                        2100L,
                        "CNY",
                        "ACTIVE",
                        OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-08T18:00:00+08:00")
                )));
        when(inventoryBalanceRepository.findById("source-sku-1"))
                .thenReturn(Optional.of(new InventoryBalanceEntity("source-sku-1", 10, 0, 0, 1L)));

        PricingInventoryModels.QuoteResult result = pricingInventoryService.quote(
                new PricingInventoryModels.QuoteCommand(
                        "quote-1",
                        List.of(new PricingInventoryModels.BusinessOrderLine("MERCHANT_OFFER_SKU", "offer-sku-1", 2))
                )
        );

        assertThat(result.totalAmount()).isEqualTo(4200L);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.businessSkuType()).isEqualTo("MERCHANT_OFFER_SKU");
            assertThat(item.businessSkuId()).isEqualTo("offer-sku-1");
            assertThat(item.sourceSkuId()).isEqualTo("source-sku-1");
            assertThat(item.supplyPrice()).isEqualTo(1800L);
            assertThat(item.merchantRetailPrice()).isEqualTo(2100L);
            assertThat(item.directRetailPrice()).isNull();
            assertThat(item.unitPrice()).isEqualTo(2100L);
            assertThat(item.sellableState()).isEqualTo("SELLABLE");
        });
        assertThat(savedSellableProjections).singleElement().satisfies(projection -> {
            assertThat(projection.getBusinessSkuType()).isEqualTo("MERCHANT_OFFER_SKU");
            assertThat(projection.getBusinessSkuId()).isEqualTo("offer-sku-1");
            assertThat(projection.getSellableState()).isEqualTo("SELLABLE");
        });
    }

    @Test
    void reserveCreatesSnapshotAndReservationForDirectSku() {
        stubSourceSkuMapping("source-sku-direct-2", "org-direct-2", "DIRECT", "ON_SHELF", "ACTIVE");
        when(organizationContextGateway.getOrganizationContext("org-direct-2"))
                .thenReturn(new OrganizationContextGateway.OrganizationContext(
                        "org-direct-2",
                        "SUPPLIER",
                        "ACTIVE",
                        "zh-CN",
                        "zh-CN,en-US",
                        true,
                        "ACTIVE"
                ));
        when(directRetailPriceRepository.findActiveBySourceSkuId("source-sku-direct-2"))
                .thenReturn(Optional.of(new DirectRetailPriceEntity(
                        "direct-price-2",
                        "source-sku-direct-2",
                        3200L,
                        "CNY",
                        "ACTIVE",
                        OffsetDateTime.parse("2026-04-08T18:00:00+08:00"),
                        OffsetDateTime.parse("2026-04-08T18:00:00+08:00")
                )));
        when(inventoryReservationRepository.findByBusinessKey("reserve-1")).thenReturn(Optional.empty());
        when(inventoryBalanceRepository.findById("source-sku-direct-2"))
                .thenReturn(Optional.of(new InventoryBalanceEntity("source-sku-direct-2", 5, 0, 0, 1L)));

        PricingInventoryModels.ReservationResult result = pricingInventoryService.reserve(
                new PricingInventoryModels.ReservationCommand(
                        "reserve-1",
                        List.of(new PricingInventoryModels.BusinessOrderLine("SOURCE_SKU", "source-sku-direct-2", 2))
                )
        );

        assertThat(result.inventoryReservationRef()).startsWith("inventory-reservation:");
        assertThat(result.pricingSnapshotRef()).startsWith("pricing-snapshot:");
        assertThat(result.totalAmount()).isEqualTo(6400L);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.businessSkuType()).isEqualTo("SOURCE_SKU");
            assertThat(item.businessSkuId()).isEqualTo("source-sku-direct-2");
            assertThat(item.directRetailPrice()).isEqualTo(3200L);
            assertThat(item.unitPrice()).isEqualTo(3200L);
        });
        assertThat(savedBalances).singleElement().satisfies(balance -> {
            assertThat(balance.getAvailableQty()).isEqualTo(3);
            assertThat(balance.getReservedQty()).isEqualTo(2);
        });
        assertThat(savedReservations).hasSize(1);
        assertThat(savedSnapshots).hasSize(1);
    }

    @Test
    void getSellableReturnsPersistedProjection() {
        when(sellableProjectionRepository.findByBusinessSkuTypeAndBusinessSkuId("MERCHANT_OFFER_SKU", "offer-sku-9"))
                .thenReturn(Optional.of(new SellableProjectionEntity(
                        "projection-9",
                        "MERCHANT_OFFER_SKU",
                        "offer-sku-9",
                        "source-sku-9",
                        "NOT_SELLABLE",
                        "PRICE_MISSING",
                        OffsetDateTime.parse("2026-04-08T18:00:00+08:00")
                )));

        PricingInventoryModels.SellableView view =
                pricingInventoryService.getSellable("MERCHANT_OFFER_SKU", "offer-sku-9");

        assertThat(view.businessSkuType()).isEqualTo("MERCHANT_OFFER_SKU");
        assertThat(view.businessSkuId()).isEqualTo("offer-sku-9");
        assertThat(view.sellableState()).isEqualTo("NOT_SELLABLE");
        assertThat(view.reasonCode()).isEqualTo("PRICE_MISSING");
    }

    private void stubMerchantOfferMapping(String businessSkuId,
                                          String sourceSkuId,
                                          String sourceProductId,
                                          String ownerId,
                                          String merchantId) {
        when(catalogSkuMappingGateway.getSkuMapping("MERCHANT_OFFER_SKU", businessSkuId))
                .thenReturn(new CatalogSkuMappingGateway.BusinessSkuMapping(
                        "MERCHANT_OFFER_SKU",
                        businessSkuId,
                        "offer-product-" + businessSkuId,
                        sourceProductId,
                        sourceSkuId,
                        "SUPPLIER",
                        ownerId,
                        "SUPPLY",
                        "relation-1",
                        merchantId,
                        "ACTIVE",
                        "ACTIVE",
                        "ACTIVE"
                ));
    }

    private void stubSourceSkuMapping(String businessSkuId,
                                      String ownerId,
                                      String sourceMode,
                                      String sourceProductStatus,
                                      String offerSkuStatus) {
        when(catalogSkuMappingGateway.getSkuMapping("SOURCE_SKU", businessSkuId))
                .thenReturn(new CatalogSkuMappingGateway.BusinessSkuMapping(
                        "SOURCE_SKU",
                        businessSkuId,
                        null,
                        "source-product-" + businessSkuId,
                        businessSkuId,
                        "SUPPLIER",
                        ownerId,
                        sourceMode,
                        null,
                        null,
                        sourceProductStatus,
                        null,
                        offerSkuStatus
                ));
    }
}
