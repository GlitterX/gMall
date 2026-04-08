package com.gmall.foundation.infrastructure.remote;

import com.gmall.foundation.application.DecorationSnapshotGateway;
import com.gmall.foundation.application.HomePageValidationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DecorationSnapshotRestClient implements DecorationSnapshotGateway {

    private final RestClient restClient;

    @Autowired
    public DecorationSnapshotRestClient(RestClient.Builder restClientBuilder,
                                        @Value("${gmall.decoration-service.base-url}") String decorationServiceBaseUrl) {
        this(restClientBuilder.baseUrl(decorationServiceBaseUrl).build());
    }

    DecorationSnapshotRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public HomePageSnapshotVerification verifyHomePageSnapshot(String storefrontId, String pageId, String terminalType) {
        try {
            SnapshotVerificationResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/storefronts/{storefrontId}/decorations/pages/{pageId}/snapshot/verification")
                            .queryParam("terminalType", terminalType)
                            .build(storefrontId, pageId))
                    .retrieve()
                    .body(SnapshotVerificationResponse.class);
            if (response == null) {
                return unavailable();
            }
            HomePageValidationStatus validationStatus = resolveValidationStatus(response.verificationStatus());
            return new HomePageSnapshotVerification(
                    validationStatus,
                    response.verificationStatus(),
                    response.snapshotId(),
                    response.snapshotVersion(),
                    response.payloadChecksum(),
                    response.checksumMatched()
            );
        } catch (RestClientResponseException exception) {
            HttpStatusCode statusCode = exception.getStatusCode();
            if (statusCode.is4xxClientError()) {
                return unreadable();
            }
            return unavailable();
        } catch (ResourceAccessException exception) {
            return unavailable();
        }
    }

    private HomePageValidationStatus resolveValidationStatus(String verificationStatus) {
        if ("CHECKSUM_MATCH".equals(verificationStatus)) {
            return HomePageValidationStatus.VALID;
        }
        if ("CHECKSUM_MISMATCH".equals(verificationStatus)) {
            return HomePageValidationStatus.SNAPSHOT_UNREADABLE;
        }
        return HomePageValidationStatus.VALIDATION_UNAVAILABLE;
    }

    private HomePageSnapshotVerification unreadable() {
        return new HomePageSnapshotVerification(
                HomePageValidationStatus.SNAPSHOT_UNREADABLE,
                null,
                null,
                null,
                null,
                false
        );
    }

    private HomePageSnapshotVerification unavailable() {
        return new HomePageSnapshotVerification(
                HomePageValidationStatus.VALIDATION_UNAVAILABLE,
                null,
                null,
                null,
                null,
                false
        );
    }

    private record SnapshotVerificationResponse(String pageId,
                                                String storefrontId,
                                                String terminalType,
                                                String snapshotId,
                                                Integer snapshotVersion,
                                                String payloadChecksum,
                                                String expectedChecksum,
                                                boolean checksumMatched,
                                                String verificationStatus) {
    }
}
