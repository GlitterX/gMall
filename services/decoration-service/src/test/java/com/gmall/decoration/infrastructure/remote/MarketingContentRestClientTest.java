package com.gmall.decoration.infrastructure.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.gmall.decoration.application.MarketingContentGateway;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MarketingContentRestClientTest {

    @Test
    void resolveObjectsReturnsResolvedMarketingObjects() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MarketingContentRestClient client = new MarketingContentRestClient(
                builder.baseUrl("http://marketing-content-service/api/marketing").build()
        );
        server.expect(requestTo("http://marketing-content-service/api/marketing/internal/marketing/objects/resolve"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        [
                          {
                            "objectId": "slot-1",
                            "objectType": "MarketingContentSlot",
                            "resolvedLocale": "zh-CN",
                            "fallbackApplied": false,
                            "publicationStatus": "LIVE",
                            "publishReady": true,
                            "payload": {
                              "slotCode": "HOME_HERO"
                            }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<MarketingContentGateway.ResolvedMarketingObject> resolvedObjects = client.resolveObjects(
                List.of(new MarketingContentGateway.MarketingObjectRef("MarketingContentSlot", "slot-1")),
                "zh-CN",
                "PUBLISH_VALIDATE",
                "MERCHANT",
                "store-1"
        );

        assertThat(resolvedObjects).singleElement().satisfies(item -> {
            assertThat(item.objectId()).isEqualTo("slot-1");
            assertThat(item.publishReady()).isTrue();
            assertThat(item.payload()).containsEntry("slotCode", "HOME_HERO");
        });
    }
}
