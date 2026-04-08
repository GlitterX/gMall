package com.gmall.catalog.infrastructure.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.gmall.catalog.application.FoundationSupplyRelationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FoundationSupplyRelationGatewayTest {

    @Test
    void resolvesAuthorizedSourceProduct() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FoundationSupplyRelationRestClient client = new FoundationSupplyRelationRestClient(
                builder.baseUrl("http://foundation-service/api/foundation").build()
        );
        server.expect(requestTo("http://foundation-service/api/foundation/internal/supply-relations/resolve?supplierOrganizationId=org-supplier&merchantOrganizationId=org-merchant&productId=source-1"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "relationId": "rel-1",
                          "active": true,
                          "catalogAuthorized": true,
                          "authorizationReason": "PRODUCT_AUTHORIZED"
                        }
                        """, MediaType.APPLICATION_JSON));

        FoundationSupplyRelationGateway.AuthorizationDecision decision =
                client.resolveSourceProductAuthorization("org-supplier", "org-merchant", "source-1");

        assertThat(decision.relationId()).isEqualTo("rel-1");
        assertThat(decision.active()).isTrue();
        assertThat(decision.catalogAuthorized()).isTrue();
        assertThat(decision.authorizationReason()).isEqualTo("PRODUCT_AUTHORIZED");
    }
}
