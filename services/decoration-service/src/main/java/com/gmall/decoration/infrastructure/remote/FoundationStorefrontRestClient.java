package com.gmall.decoration.infrastructure.remote;

import com.gmall.decoration.application.FoundationStorefrontGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class FoundationStorefrontRestClient implements FoundationStorefrontGateway {

    private final RestClient restClient;

    @Autowired
    public FoundationStorefrontRestClient(RestClient.Builder restClientBuilder,
                                          @Value("${gmall.foundation-service.base-url}") String foundationBaseUrl) {
        this(restClientBuilder.baseUrl(foundationBaseUrl).build());
    }

    FoundationStorefrontRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public StorefrontView getStorefront(String storefrontId) {
        try {
            StorefrontResponse response = restClient.get()
                    .uri("/storefronts/{storefrontId}", storefrontId)
                    .retrieve()
                    .body(StorefrontResponse.class);
            if (response == null) {
                throw new IllegalStateException("店铺结果为空");
            }
            return new StorefrontView(
                    response.storefrontId(),
                    response.storefrontType(),
                    response.status(),
                    response.defaultLocale(),
                    response.supportedLocales()
            );
        } catch (HttpClientErrorException exception) {
            throw new DecorationRemoteException("店铺读取失败: " + storefrontId, exception);
        }
    }

    @Override
    public StorefrontOperability getOperability(String storefrontId, String terminalType, String operation) {
        try {
            OperabilityResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/storefronts/{storefrontId}/operability")
                            .queryParam("terminalType", terminalType)
                            .queryParam("operation", operation)
                            .build(storefrontId))
                    .retrieve()
                    .body(OperabilityResponse.class);
            if (response == null) {
                throw new IllegalStateException("店铺可操作性结果为空");
            }
            return new StorefrontOperability(
                    response.storefrontId(),
                    response.terminalType(),
                    response.storefrontStatus(),
                    response.storefrontActive(),
                    response.permissionBindingReady(),
                    response.terminalEnabled(),
                    response.homePageBound(),
                    response.operable(),
                    response.homePageValidationStatus(),
                    response.homePageValidationPassed(),
                    response.allowed()
            );
        } catch (HttpClientErrorException exception) {
            throw new DecorationRemoteException("店铺可操作性读取失败: " + storefrontId, exception);
        }
    }

    private record StorefrontResponse(String storefrontId,
                                      String organizationId,
                                      String sellerId,
                                      String storefrontType,
                                      String status,
                                      String defaultLocale,
                                      String supportedLocales,
                                      long aggregateVersion) {
    }

    private record OperabilityResponse(String storefrontId,
                                       String terminalType,
                                       String storefrontStatus,
                                       boolean storefrontActive,
                                       boolean permissionBindingReady,
                                       boolean terminalEnabled,
                                       boolean homePageBound,
                                       boolean operable,
                                       String homePageValidationStatus,
                                       boolean homePageValidationPassed,
                                       long storefrontVersion,
                                       long terminalVersion,
                                       long bindingVersion,
                                       boolean allowed) {
    }
}
