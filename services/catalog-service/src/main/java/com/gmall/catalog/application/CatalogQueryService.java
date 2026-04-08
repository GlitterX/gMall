package com.gmall.catalog.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmall.catalog.infrastructure.persistence.CatalogProjectionEntity;
import com.gmall.catalog.infrastructure.persistence.CatalogProjectionRepository;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductEntity;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferProductRepository;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferSkuEntity;
import com.gmall.catalog.infrastructure.persistence.MerchantOfferSkuRepository;
import com.gmall.catalog.infrastructure.persistence.SourceProductEntity;
import com.gmall.catalog.infrastructure.persistence.SourceProductRepository;
import com.gmall.catalog.infrastructure.persistence.SourceSkuEntity;
import com.gmall.catalog.infrastructure.persistence.SourceSkuRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogQueryService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String DEFAULT_SORT_BY = "updatedAt";
    private static final String DEFAULT_SORT_DIRECTION = "DESC";

    private final CatalogProjectionRepository catalogProjectionRepository;
    private final MerchantOfferSkuRepository merchantOfferSkuRepository;
    private final MerchantOfferProductRepository merchantOfferProductRepository;
    private final SourceProductRepository sourceProductRepository;
    private final SourceSkuRepository sourceSkuRepository;
    private final ObjectMapper objectMapper;

    public CatalogQueryService(CatalogProjectionRepository catalogProjectionRepository,
                               MerchantOfferSkuRepository merchantOfferSkuRepository,
                               MerchantOfferProductRepository merchantOfferProductRepository,
                               SourceProductRepository sourceProductRepository,
                               SourceSkuRepository sourceSkuRepository,
                               ObjectMapper objectMapper) {
        this.catalogProjectionRepository = catalogProjectionRepository;
        this.merchantOfferSkuRepository = merchantOfferSkuRepository;
        this.merchantOfferProductRepository = merchantOfferProductRepository;
        this.sourceProductRepository = sourceProductRepository;
        this.sourceSkuRepository = sourceSkuRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.ProductDetailView getProduct(String productViewId, String locale) {
        CatalogProjectionEntity projection = findProjection("MALL", productViewId, locale);
        CatalogProjectionPayload payload = readPayload(projection.getProjectionPayload());
        return new CatalogQueryModels.ProductDetailView(
                projection.getProductViewId(),
                projection.getBusinessProductId(),
                projection.getSourceProductId(),
                projection.getMerchantOfferProductId(),
                payload.productType(),
                payload.title(),
                payload.coverImage(),
                payload.productStatus(),
                payload.sourceType(),
                payload.resolvedLocale(),
                payload.fallbackApplied(),
                payload.contentCompleteness()
        );
    }

    @Transactional(readOnly = true)
    public List<CatalogQueryModels.DecorationProductView> getDecorationProducts(List<String> productViewIds, String locale) {
        List<CatalogQueryModels.DecorationProductView> result = new ArrayList<>();
        for (String productViewId : productViewIds) {
            CatalogProjectionEntity projection = findProjection("DECORATION", productViewId, locale);
            CatalogProjectionPayload payload = readPayload(projection.getProjectionPayload());
            result.add(new CatalogQueryModels.DecorationProductView(
                    projection.getProductViewId(),
                    payload.title(),
                    payload.coverImage(),
                    payload.productStatus(),
                    payload.sourceType(),
                    payload.resolvedLocale(),
                    payload.fallbackApplied(),
                    payload.contentCompleteness()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.OfferSkuMappingView getOfferSkuMapping(String merchantOfferSkuId) {
        MerchantOfferSkuEntity offerSku = merchantOfferSkuRepository.findById(merchantOfferSkuId)
                .orElseThrow(() -> new ProductNotFoundException("经营 SKU 不存在: " + merchantOfferSkuId));
        MerchantOfferProductEntity offerProduct = merchantOfferProductRepository.findById(offerSku.getMerchantOfferProductId())
                .orElseThrow(() -> new ProductNotFoundException("经营商品不存在: " + offerSku.getMerchantOfferProductId()));
        SourceProductEntity sourceProduct = sourceProductRepository.findById(offerProduct.getSourceProductId())
                .orElseThrow(() -> new ProductNotFoundException("源商品不存在: " + offerProduct.getSourceProductId()));
        return new CatalogQueryModels.OfferSkuMappingView(
                offerProduct.getMerchantOfferProductId(),
                offerSku.getMerchantOfferSkuId(),
                sourceProduct.getSourceProductId(),
                offerSku.getSourceSkuId(),
                offerProduct.getRelationId(),
                offerProduct.getMerchantId(),
                sourceProduct.getProductStatus(),
                offerProduct.getOfferStatus(),
                offerSku.getOfferSkuStatus()
        );
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.BusinessSkuMappingView getBusinessSkuMapping(String businessSkuType, String businessSkuId) {
        if ("SOURCE_SKU".equals(businessSkuType)) {
            SourceSkuEntity sourceSku = sourceSkuRepository.findById(businessSkuId)
                    .orElseThrow(() -> new ProductNotFoundException("源 SKU 不存在: " + businessSkuId));
            SourceProductEntity sourceProduct = sourceProductRepository.findById(sourceSku.getSourceProductId())
                    .orElseThrow(() -> new ProductNotFoundException("源商品不存在: " + sourceSku.getSourceProductId()));
            return new CatalogQueryModels.BusinessSkuMappingView(
                    businessSkuType,
                    businessSkuId,
                    null,
                    sourceProduct.getSourceProductId(),
                    sourceSku.getSourceSkuId(),
                    sourceProduct.getOwnerType(),
                    sourceProduct.getOwnerId(),
                    sourceProduct.getSourceMode(),
                    null,
                    null,
                    sourceProduct.getProductStatus(),
                    null,
                    sourceSku.getSkuStatus()
            );
        }
        if ("MERCHANT_OFFER_SKU".equals(businessSkuType)) {
            MerchantOfferSkuEntity offerSku = merchantOfferSkuRepository.findById(businessSkuId)
                    .orElseThrow(() -> new ProductNotFoundException("经营 SKU 不存在: " + businessSkuId));
            MerchantOfferProductEntity offerProduct = merchantOfferProductRepository.findById(offerSku.getMerchantOfferProductId())
                    .orElseThrow(() -> new ProductNotFoundException("经营商品不存在: " + offerSku.getMerchantOfferProductId()));
            SourceProductEntity sourceProduct = sourceProductRepository.findById(offerProduct.getSourceProductId())
                    .orElseThrow(() -> new ProductNotFoundException("源商品不存在: " + offerProduct.getSourceProductId()));
            return new CatalogQueryModels.BusinessSkuMappingView(
                    businessSkuType,
                    businessSkuId,
                    offerProduct.getMerchantOfferProductId(),
                    sourceProduct.getSourceProductId(),
                    offerSku.getSourceSkuId(),
                    sourceProduct.getOwnerType(),
                    sourceProduct.getOwnerId(),
                    sourceProduct.getSourceMode(),
                    offerProduct.getRelationId(),
                    offerProduct.getMerchantId(),
                    sourceProduct.getProductStatus(),
                    offerProduct.getOfferStatus(),
                    offerSku.getOfferSkuStatus()
            );
        }
        throw new IllegalArgumentException("不支持的 businessSkuType: " + businessSkuType);
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.SourceProductManagementView getSourceProductManagement(String sourceProductId) {
        SourceProductEntity sourceProduct = sourceProductRepository.findById(sourceProductId)
                .orElseThrow(() -> new ProductNotFoundException("源商品不存在: " + sourceProductId));
        List<CatalogQueryModels.SourceSkuView> skus = sourceSkuRepository.findBySourceProductId(sourceProductId).stream()
                .map(sku -> new CatalogQueryModels.SourceSkuView(
                        sku.getSourceSkuId(),
                        sku.getSkuStatus(),
                        sku.getVersion()
                ))
                .toList();
        return new CatalogQueryModels.SourceProductManagementView(
                sourceProduct.getSourceProductId(),
                sourceProduct.getProductViewId(),
                sourceProduct.getOwnerType(),
                sourceProduct.getOwnerId(),
                sourceProduct.getSourceMode(),
                sourceProduct.getCategoryId(),
                sourceProduct.getBrandId(),
                sourceProduct.getProductStatus(),
                sourceProduct.getContentVersion(),
                skus
        );
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.PageResult<CatalogQueryModels.SourceProductSummaryView> listSourceProductManagement(String ownerId,
                                                                                                                    String ownerType,
                                                                                                                    String sourceMode,
                                                                                                                    String productStatus,
                                                                                                                    Integer pageNo,
                                                                                                                    Integer pageSize,
                                                                                                                    String sortBy,
                                                                                                                    String sortDirection) {
        if (!hasText(ownerId)) {
            throw new IllegalArgumentException("ownerId 不能为空");
        }
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedSortBy = normalizeSourceSortBy(sortBy);
        String normalizedSortDirection = normalizeSortDirection(sortDirection);
        Stream<SourceProductEntity> stream = sourceProductRepository.findByOwnerId(ownerId).stream();
        if (hasText(ownerType)) {
            stream = stream.filter(item -> ownerType.equals(item.getOwnerType()));
        }
        if (hasText(sourceMode)) {
            stream = stream.filter(item -> sourceMode.equals(item.getSourceMode()));
        }
        if (hasText(productStatus)) {
            stream = stream.filter(item -> productStatus.equals(item.getProductStatus()));
        }
        Comparator<SourceProductEntity> comparator = buildSourceComparator(normalizedSortBy, normalizedSortDirection);
        List<CatalogQueryModels.SourceProductSummaryView> filtered = stream.sorted(comparator)
                .map(item -> new CatalogQueryModels.SourceProductSummaryView(
                        item.getSourceProductId(),
                        item.getProductViewId(),
                        item.getOwnerType(),
                        item.getOwnerId(),
                        item.getSourceMode(),
                        item.getProductStatus(),
                        item.getContentVersion()
                ))
                .toList();
        return buildPageResult(filtered, normalizedPageNo, normalizedPageSize, normalizedSortBy, normalizedSortDirection);
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.PageResult<CatalogQueryModels.SourceProductSummaryView> listSourceProductManagement(String ownerId,
                                                                                                                    String ownerType,
                                                                                                                    String sourceMode,
                                                                                                                    String productStatus) {
        return listSourceProductManagement(
                ownerId,
                ownerType,
                sourceMode,
                productStatus,
                DEFAULT_PAGE_NO,
                DEFAULT_PAGE_SIZE,
                DEFAULT_SORT_BY,
                DEFAULT_SORT_DIRECTION
        );
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.MerchantOfferProductManagementView getMerchantOfferProductManagement(String merchantOfferProductId) {
        MerchantOfferProductEntity offerProduct = merchantOfferProductRepository.findById(merchantOfferProductId)
                .orElseThrow(() -> new ProductNotFoundException("经营商品不存在: " + merchantOfferProductId));
        List<CatalogQueryModels.MerchantOfferSkuView> skus = merchantOfferSkuRepository.findByMerchantOfferProductId(merchantOfferProductId)
                .stream()
                .map(sku -> new CatalogQueryModels.MerchantOfferSkuView(
                        sku.getMerchantOfferSkuId(),
                        sku.getSourceSkuId(),
                        sku.getOfferSkuStatus(),
                        sku.getMappingVersion()
                ))
                .toList();
        return new CatalogQueryModels.MerchantOfferProductManagementView(
                offerProduct.getMerchantOfferProductId(),
                offerProduct.getProductViewId(),
                offerProduct.getMerchantId(),
                offerProduct.getRelationId(),
                offerProduct.getSourceProductId(),
                offerProduct.getOfferStatus(),
                offerProduct.getSyncConfirmationStatus(),
                skus
        );
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.PageResult<CatalogQueryModels.MerchantOfferProductSummaryView> listMerchantOfferProductManagement(String merchantId,
                                                                                                                               String relationId,
                                                                                                                               String offerStatus,
                                                                                                                               Integer pageNo,
                                                                                                                               Integer pageSize,
                                                                                                                               String sortBy,
                                                                                                                               String sortDirection) {
        if (!hasText(merchantId)) {
            throw new IllegalArgumentException("merchantId 不能为空");
        }
        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedSortBy = normalizeOfferSortBy(sortBy);
        String normalizedSortDirection = normalizeSortDirection(sortDirection);
        Stream<MerchantOfferProductEntity> stream = merchantOfferProductRepository.findByMerchantId(merchantId).stream();
        if (hasText(relationId)) {
            stream = stream.filter(item -> relationId.equals(item.getRelationId()));
        }
        if (hasText(offerStatus)) {
            stream = stream.filter(item -> offerStatus.equals(item.getOfferStatus()));
        }
        Comparator<MerchantOfferProductEntity> comparator = buildOfferComparator(normalizedSortBy, normalizedSortDirection);
        List<CatalogQueryModels.MerchantOfferProductSummaryView> filtered = stream.sorted(comparator)
                .map(item -> new CatalogQueryModels.MerchantOfferProductSummaryView(
                        item.getMerchantOfferProductId(),
                        item.getProductViewId(),
                        item.getMerchantId(),
                        item.getRelationId(),
                        item.getSourceProductId(),
                        item.getOfferStatus(),
                        item.getSyncConfirmationStatus()
                ))
                .toList();
        return buildPageResult(filtered, normalizedPageNo, normalizedPageSize, normalizedSortBy, normalizedSortDirection);
    }

    @Transactional(readOnly = true)
    public CatalogQueryModels.PageResult<CatalogQueryModels.MerchantOfferProductSummaryView> listMerchantOfferProductManagement(String merchantId,
                                                                                                                               String relationId,
                                                                                                                               String offerStatus) {
        return listMerchantOfferProductManagement(
                merchantId,
                relationId,
                offerStatus,
                DEFAULT_PAGE_NO,
                DEFAULT_PAGE_SIZE,
                DEFAULT_SORT_BY,
                DEFAULT_SORT_DIRECTION
        );
    }

    private CatalogProjectionEntity findProjection(String presentationType, String productViewId, String locale) {
        return catalogProjectionRepository.findByPresentationTypeAndProductViewIdAndLocale(
                        presentationType,
                        productViewId,
                        locale
                )
                .or(() -> catalogProjectionRepository
                        .findByPresentationTypeAndProductViewIdOrderByUpdatedAtDesc(presentationType, productViewId)
                        .stream()
                        .findFirst())
                .orElseThrow(() -> new ProductNotFoundException(productViewId));
    }

    private CatalogProjectionPayload readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, CatalogProjectionPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析商品投影", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int normalizePageNo(Integer pageNo) {
        int normalized = pageNo == null ? DEFAULT_PAGE_NO : pageNo;
        if (normalized < 1) {
            throw new IllegalArgumentException("pageNo 必须 >= 1");
        }
        return normalized;
    }

    private int normalizePageSize(Integer pageSize) {
        int normalized = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (normalized < 1 || normalized > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        return normalized;
    }

    private String normalizeSortDirection(String sortDirection) {
        if (!hasText(sortDirection)) {
            return DEFAULT_SORT_DIRECTION;
        }
        String normalized = sortDirection.trim().toUpperCase();
        if (!"ASC".equals(normalized) && !"DESC".equals(normalized)) {
            throw new IllegalArgumentException("sortDirection 仅支持 ASC 或 DESC");
        }
        return normalized;
    }

    private String normalizeSourceSortBy(String sortBy) {
        if (!hasText(sortBy)) {
            return DEFAULT_SORT_BY;
        }
        String normalized = sortBy.trim();
        return switch (normalized) {
            case "updatedAt", "contentVersion", "sourceProductId", "productStatus" -> normalized;
            default -> throw new IllegalArgumentException("source-products 不支持的 sortBy: " + normalized);
        };
    }

    private String normalizeOfferSortBy(String sortBy) {
        if (!hasText(sortBy)) {
            return DEFAULT_SORT_BY;
        }
        String normalized = sortBy.trim();
        return switch (normalized) {
            case "updatedAt", "merchantOfferProductId", "offerStatus", "syncConfirmationStatus" -> normalized;
            default -> throw new IllegalArgumentException("offer-products 不支持的 sortBy: " + normalized);
        };
    }

    private Comparator<SourceProductEntity> buildSourceComparator(String sortBy, String sortDirection) {
        Comparator<SourceProductEntity> comparator = switch (sortBy) {
            case "contentVersion" -> Comparator.comparingLong(SourceProductEntity::getContentVersion);
            case "sourceProductId" -> Comparator.comparing(SourceProductEntity::getSourceProductId);
            case "productStatus" -> Comparator.comparing(SourceProductEntity::getProductStatus);
            default -> Comparator.comparing(SourceProductEntity::getUpdatedAt);
        };
        if ("DESC".equals(sortDirection)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private Comparator<MerchantOfferProductEntity> buildOfferComparator(String sortBy, String sortDirection) {
        Comparator<MerchantOfferProductEntity> comparator = switch (sortBy) {
            case "merchantOfferProductId" -> Comparator.comparing(MerchantOfferProductEntity::getMerchantOfferProductId);
            case "offerStatus" -> Comparator.comparing(MerchantOfferProductEntity::getOfferStatus);
            case "syncConfirmationStatus" -> Comparator.comparing(MerchantOfferProductEntity::getSyncConfirmationStatus);
            default -> Comparator.comparing(MerchantOfferProductEntity::getUpdatedAt);
        };
        if ("DESC".equals(sortDirection)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private <T> CatalogQueryModels.PageResult<T> buildPageResult(List<T> allItems,
                                                                 int pageNo,
                                                                 int pageSize,
                                                                 String sortBy,
                                                                 String sortDirection) {
        long total = allItems.size();
        int totalPages = total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
        long fromIndex = (long) (pageNo - 1) * pageSize;
        List<T> pageItems = fromIndex >= total
                ? List.of()
                : allItems.stream()
                .skip(fromIndex)
                .limit(pageSize)
                .toList();
        boolean hasNext = pageNo < totalPages;
        return new CatalogQueryModels.PageResult<>(
                pageNo,
                pageSize,
                total,
                totalPages,
                hasNext,
                sortBy,
                sortDirection,
                pageItems
        );
    }
}
