package com.gmall.pricinginventory.application;

public interface CatalogSkuMappingGateway {

    BusinessSkuMapping getSkuMapping(String businessSkuType, String businessSkuId);

    record BusinessSkuMapping(String businessSkuType,
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
}
