package com.gmall.pricinginventory.infrastructure.remote;

import com.gmall.pricinginventory.application.CatalogSkuMappingGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class CatalogSkuMappingRestClient implements CatalogSkuMappingGateway {

    private final RestClient restClient;

    @Autowired
    public CatalogSkuMappingRestClient(RestClient.Builder restClientBuilder,
                                       @Value("${gmall.catalog-service.base-url}") String catalogBaseUrl) {
        this(restClientBuilder.baseUrl(catalogBaseUrl).build());
    }

    CatalogSkuMappingRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public BusinessSkuMapping getSkuMapping(String businessSkuType, String businessSkuId) {
        try {
            Response response = restClient.get()
                    .uri("/internal/skus/{businessSkuType}/{businessSkuId}/mapping", businessSkuType, businessSkuId)
                    .retrieve()
                    .body(Response.class);
            if (response == null) {
                throw new IllegalStateException("catalog SKU 映射结果为空");
            }
            return new BusinessSkuMapping(
                    response.businessSkuType(),
                    response.businessSkuId(),
                    response.merchantOfferProductId(),
                    response.sourceProductId(),
                    response.sourceSkuId(),
                    response.ownerType(),
                    response.ownerId(),
                    response.sourceMode(),
                    response.relationId(),
                    response.merchantId(),
                    response.sourceProductStatus(),
                    response.offerProductStatus(),
                    response.offerSkuStatus()
            );
        } catch (HttpClientErrorException exception) {
            throw new PricingInventoryRemoteException(
                    "CATALOG_MAPPING_MISSING",
                    "CATALOG_MAPPING_MISSING: " + businessSkuType + ":" + businessSkuId,
                    exception
            );
        }
    }

    private record Response(String businessSkuType,
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
