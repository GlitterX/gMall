package com.gmall.catalog.infrastructure.remote;

import com.gmall.catalog.application.FoundationSupplyRelationGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FoundationSupplyRelationRestClient implements FoundationSupplyRelationGateway {

    private final RestClient restClient;

    @Autowired
    public FoundationSupplyRelationRestClient(RestClient.Builder restClientBuilder,
                                              @Value("${gmall.foundation-service.base-url}") String foundationBaseUrl) {
        this(restClientBuilder.baseUrl(foundationBaseUrl).build());
    }

    FoundationSupplyRelationRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public AuthorizationDecision resolveSourceProductAuthorization(String supplierOrganizationId,
                                                                   String merchantOrganizationId,
                                                                   String sourceProductId) {
        ResolutionResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/supply-relations/resolve")
                        .queryParam("supplierOrganizationId", supplierOrganizationId)
                        .queryParam("merchantOrganizationId", merchantOrganizationId)
                        .queryParam("productId", sourceProductId)
                        .build())
                .retrieve()
                .body(ResolutionResponse.class);
        if (response == null) {
            throw new IllegalStateException("foundation 供货关系解析结果为空");
        }
        return new AuthorizationDecision(
                response.relationId(),
                Boolean.TRUE.equals(response.active()),
                Boolean.TRUE.equals(response.catalogAuthorized()),
                response.authorizationReason()
        );
    }

    private record ResolutionResponse(String relationId,
                                      Boolean active,
                                      Boolean catalogAuthorized,
                                      String authorizationReason) {
    }
}
