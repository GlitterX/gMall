package com.gmall.mallbff.infrastructure.remote;

import com.gmall.mallbff.application.MarketingProjectionGateway;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MarketingProjectionRestClient implements MarketingProjectionGateway {

    private final RestClient restClient;

    @Autowired
    public MarketingProjectionRestClient(@Value("${gmall.marketing-content-service.base-url}") String marketingBaseUrl) {
        this(RestClient.builder().baseUrl(marketingBaseUrl).build());
    }

    MarketingProjectionRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ProjectionView getProjection(String projectionType,
                                        String ownerType,
                                        String ownerId,
                                        String terminalType,
                                        String locale,
                                        String pageContext) {
        try {
            Response response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/marketing/projections")
                            .queryParam("projectionType", projectionType)
                            .queryParam("ownerType", ownerType)
                            .queryParam("ownerId", ownerId)
                            .queryParam("terminalType", terminalType)
                            .queryParam("locale", locale)
                            .queryParam("pageContext", pageContext)
                            .build()
                    )
                    .retrieve()
                    .body(Response.class);
            if (response == null) {
                return new ProjectionView(List.of(), locale, false, 0);
            }
            return new ProjectionView(
                    response.items() == null ? List.of() : response.items(),
                    response.resolvedLocale(),
                    response.fallbackApplied(),
                    response.cacheTtlSeconds()
            );
        } catch (RestClientResponseException exception) {
            throw new MallBffRemoteException("读取营销投影失败", exception);
        }
    }

    private record Response(List<Map<String, Object>> items,
                            String resolvedLocale,
                            boolean fallbackApplied,
                            int cacheTtlSeconds) {
    }
}
