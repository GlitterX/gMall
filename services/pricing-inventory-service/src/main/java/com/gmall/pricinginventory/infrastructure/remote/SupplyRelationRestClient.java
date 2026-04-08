package com.gmall.pricinginventory.infrastructure.remote;

import com.gmall.pricinginventory.application.SupplyRelationGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class SupplyRelationRestClient implements SupplyRelationGateway {

    private final RestClient restClient;

    @Autowired
    public SupplyRelationRestClient(RestClient.Builder restClientBuilder,
                                    @Value("${gmall.foundation-service.base-url}") String foundationBaseUrl) {
        this(restClientBuilder.baseUrl(foundationBaseUrl).build());
    }

    SupplyRelationRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public SupplyRelationContext resolveSupplyRelation(String supplierOrganizationId,
                                                       String merchantOrganizationId,
                                                       String sourceProductId) {
        try {
            Response response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/supply-relations/resolve")
                            .queryParam("supplierOrganizationId", supplierOrganizationId)
                            .queryParam("merchantOrganizationId", merchantOrganizationId)
                            .queryParam("productId", sourceProductId)
                            .build())
                    .retrieve()
                    .body(Response.class);
            if (response == null) {
                throw new IllegalStateException("供货关系解析结果为空");
            }
            return new SupplyRelationContext(
                    response.relationId(),
                    response.status(),
                    response.active(),
                    Boolean.TRUE.equals(response.catalogAuthorized()),
                    response.authorizationReason()
            );
        } catch (HttpClientErrorException exception) {
            throw new PricingInventoryRemoteException(
                    "SUPPLY_RELATION_MISSING",
                    "SUPPLY_RELATION_MISSING: " + supplierOrganizationId + "->" + merchantOrganizationId,
                    exception
            );
        }
    }

    private record Response(String relationId,
                            String status,
                            boolean active,
                            Boolean catalogAuthorized,
                            String authorizationReason) {
    }
}
