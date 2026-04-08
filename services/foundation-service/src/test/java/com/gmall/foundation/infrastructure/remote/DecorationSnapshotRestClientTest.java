package com.gmall.foundation.infrastructure.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.gmall.foundation.application.DecorationSnapshotGateway;
import com.gmall.foundation.application.HomePageValidationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DecorationSnapshotRestClientTest {

    @Test
    void returnsValidWhenVerificationInterfaceReturnsChecksumMatch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DecorationSnapshotRestClient client = new DecorationSnapshotRestClient(
                builder.baseUrl("http://decoration-service").build()
        );
        server.expect(requestTo("http://decoration-service/internal/storefronts/store-1/decorations/pages/home-mobile/snapshot/verification?terminalType=MOBILE"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "pageId": "home-mobile",
                          "snapshotId": "snapshot-1",
                          "snapshotVersion": 2,
                          "payloadChecksum": "checksum-1",
                          "verificationStatus": "CHECKSUM_MATCH",
                          "checksumMatched": true
                        }
                        """, MediaType.APPLICATION_JSON));

        DecorationSnapshotGateway.HomePageSnapshotVerification verification =
                client.verifyHomePageSnapshot("store-1", "home-mobile", "MOBILE");

        assertThat(verification.validationStatus()).isEqualTo(HomePageValidationStatus.VALID);
        assertThat(verification.verificationStatus()).isEqualTo("CHECKSUM_MATCH");
        assertThat(verification.snapshotId()).isEqualTo("snapshot-1");
        assertThat(verification.payloadChecksum()).isEqualTo("checksum-1");
    }

    @Test
    void returnsUnreadableWhenSnapshotIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DecorationSnapshotRestClient client = new DecorationSnapshotRestClient(
                builder.baseUrl("http://decoration-service").build()
        );
        server.expect(requestTo("http://decoration-service/internal/storefronts/store-1/decorations/pages/home-mobile/snapshot/verification?terminalType=MOBILE"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        DecorationSnapshotGateway.HomePageSnapshotVerification verification =
                client.verifyHomePageSnapshot("store-1", "home-mobile", "MOBILE");

        assertThat(verification.validationStatus()).isEqualTo(HomePageValidationStatus.SNAPSHOT_UNREADABLE);
    }

    @Test
    void returnsUnavailableWhenDecorationServiceFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DecorationSnapshotRestClient client = new DecorationSnapshotRestClient(
                builder.baseUrl("http://decoration-service").build()
        );
        server.expect(requestTo("http://decoration-service/internal/storefronts/store-1/decorations/pages/home-mobile/snapshot/verification?terminalType=MOBILE"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        DecorationSnapshotGateway.HomePageSnapshotVerification verification =
                client.verifyHomePageSnapshot("store-1", "home-mobile", "MOBILE");

        assertThat(verification.validationStatus()).isEqualTo(HomePageValidationStatus.VALIDATION_UNAVAILABLE);
    }

    @Test
    void returnsUnreadableWhenVerificationStatusIsMismatch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DecorationSnapshotRestClient client = new DecorationSnapshotRestClient(
                builder.baseUrl("http://decoration-service").build()
        );
        server.expect(requestTo("http://decoration-service/internal/storefronts/store-1/decorations/pages/home-mobile/snapshot/verification?terminalType=MOBILE"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "pageId": "home-mobile",
                          "snapshotId": "snapshot-2",
                          "snapshotVersion": 3,
                          "payloadChecksum": "checksum-2",
                          "verificationStatus": "CHECKSUM_MISMATCH",
                          "checksumMatched": false
                        }
                        """, MediaType.APPLICATION_JSON));

        DecorationSnapshotGateway.HomePageSnapshotVerification verification =
                client.verifyHomePageSnapshot("store-1", "home-mobile", "MOBILE");

        assertThat(verification.validationStatus()).isEqualTo(HomePageValidationStatus.SNAPSHOT_UNREADABLE);
        assertThat(verification.verificationStatus()).isEqualTo("CHECKSUM_MISMATCH");
    }
}
