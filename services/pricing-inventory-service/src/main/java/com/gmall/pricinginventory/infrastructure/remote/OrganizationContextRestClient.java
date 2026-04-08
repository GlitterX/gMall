package com.gmall.pricinginventory.infrastructure.remote;

import com.gmall.pricinginventory.application.OrganizationContextGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class OrganizationContextRestClient implements OrganizationContextGateway {

    private final RestClient restClient;

    @Autowired
    public OrganizationContextRestClient(RestClient.Builder restClientBuilder,
                                         @Value("${gmall.foundation-service.base-url}") String foundationBaseUrl) {
        this(restClientBuilder.baseUrl(foundationBaseUrl).build());
    }

    OrganizationContextRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public OrganizationContext getOrganizationContext(String organizationId) {
        try {
            Response response = restClient.get()
                    .uri("/internal/organizations/{organizationId}/context", organizationId)
                    .retrieve()
                    .body(Response.class);
            if (response == null) {
                throw new IllegalStateException("组织上下文结果为空");
            }
            return new OrganizationContext(
                    response.organizationId(),
                    response.organizationType(),
                    response.status(),
                    response.defaultLocale(),
                    response.supportedLocales(),
                    response.directSupplierQualified(),
                    response.directSupplierQualificationStatus()
            );
        } catch (HttpClientErrorException exception) {
            throw new PricingInventoryRemoteException(
                    "ORGANIZATION_CONTEXT_MISSING",
                    "ORGANIZATION_CONTEXT_MISSING: " + organizationId,
                    exception
            );
        }
    }

    private record Response(String organizationId,
                            String organizationType,
                            String status,
                            String defaultLocale,
                            String supportedLocales,
                            boolean directSupplierQualified,
                            String directSupplierQualificationStatus) {
    }
}
