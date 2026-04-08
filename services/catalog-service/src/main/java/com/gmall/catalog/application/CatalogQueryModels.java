package com.gmall.catalog.application;

public final class CatalogQueryModels {

    private CatalogQueryModels() {
    }

    public record SourceProductView(String sourceProductId,
                                    String productViewId,
                                    String ownerType,
                                    String ownerId,
                                    String sourceMode,
                                    String categoryId,
                                    String brandId,
                                    String productStatus,
                                    long contentVersion) {
    }

    public record MerchantOfferProductView(String merchantOfferProductId,
                                           String productViewId,
                                           String merchantId,
                                           String relationId,
                                           String sourceProductId,
                                           String offerStatus) {
    }

    public record ProductDetailView(String productViewId,
                                    String businessProductId,
                                    String sourceProductId,
                                    String merchantOfferProductId,
                                    String productType,
                                    String title,
                                    String coverImage,
                                    String productStatus,
                                    String sourceType,
                                    String resolvedLocale,
                                    boolean fallbackApplied,
                                    String contentCompleteness) {
    }

    public record DecorationProductView(String productViewId,
                                        String title,
                                        String coverImage,
                                        String productStatus,
                                        String sourceType,
                                        String resolvedLocale,
                                        boolean fallbackApplied,
                                        String contentCompleteness) {
    }

    public record SourceSkuView(String sourceSkuId,
                                String skuStatus,
                                long version) {
    }

    public record MerchantOfferSkuView(String merchantOfferSkuId,
                                       String sourceSkuId,
                                       String offerSkuStatus,
                                       long mappingVersion) {
    }

    public record SourceProductManagementView(String sourceProductId,
                                              String productViewId,
                                              String ownerType,
                                              String ownerId,
                                              String sourceMode,
                                              String categoryId,
                                              String brandId,
                                              String productStatus,
                                              long contentVersion,
                                              java.util.List<SourceSkuView> skus) {
    }

    public record SourceProductSummaryView(String sourceProductId,
                                           String productViewId,
                                           String ownerType,
                                           String ownerId,
                                           String sourceMode,
                                           String productStatus,
                                           long contentVersion) {
    }

    public record MerchantOfferProductManagementView(String merchantOfferProductId,
                                                     String productViewId,
                                                     String merchantId,
                                                     String relationId,
                                                     String sourceProductId,
                                                     String offerStatus,
                                                     String syncConfirmationStatus,
                                                     java.util.List<MerchantOfferSkuView> skus) {
    }

    public record MerchantOfferProductSummaryView(String merchantOfferProductId,
                                                  String productViewId,
                                                  String merchantId,
                                                  String relationId,
                                                  String sourceProductId,
                                                  String offerStatus,
                                                  String syncConfirmationStatus) {
    }

    public record PageResult<T>(int pageNo,
                                int pageSize,
                                long total,
                                int totalPages,
                                boolean hasNext,
                                String sortBy,
                                String sortDirection,
                                java.util.List<T> items) {
    }

    public record BusinessSkuMappingView(String businessSkuType,
                                         String businessSkuId,
                                         String merchantOfferProductId,
                                         String sourceProductId,
                                         String sourceSkuId,
                                         String ownerType,
                                         String ownerId,
                                         String sourceMode,
                                         String relationId,
                                         String merchantId,
                                         String sourceProductStatus,
                                         String offerProductStatus,
                                         String offerSkuStatus) {
    }

    public record OfferSkuMappingView(String merchantOfferProductId,
                                      String merchantOfferSkuId,
                                      String sourceProductId,
                                      String sourceSkuId,
                                      String relationId,
                                      String merchantId,
                                      String sourceProductStatus,
                                      String offerProductStatus,
                                      String offerSkuStatus) {
    }
}
