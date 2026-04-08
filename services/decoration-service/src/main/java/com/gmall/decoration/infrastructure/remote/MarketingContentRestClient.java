package com.gmall.decoration.infrastructure.remote;

import com.gmall.decoration.application.MarketingContentGateway;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MarketingContentRestClient implements MarketingContentGateway {

    private final RestClient restClient;

    @Autowired
    public MarketingContentRestClient(RestClient.Builder restClientBuilder,
                                      @Value("${gmall.marketing-content-service.base-url}") String marketingContentBaseUrl) {
        this(restClientBuilder.baseUrl(marketingContentBaseUrl).build());
    }

    MarketingContentRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<ResolvedMarketingObject> resolveObjects(List<MarketingObjectRef> objectRefs,
                                                        String locale,
                                                        String resolveMode,
                                                        String ownerType,
                                                        String ownerId) {
        try {
            Response[] response = restClient.post()
                    .uri("/internal/marketing/objects/resolve")
                    .body(new Request(
                            locale,
                            resolveMode,
                            new OwnerContext(ownerType, ownerId),
                            objectRefs.stream()
                                    .map(item -> new ObjectRef(item.objectType(), item.objectId()))
                                    .toList()
                    ))
                    .retrieve()
                    .body(Response[].class);
            if (response == null) {
                return List.of();
            }
            return java.util.Arrays.stream(response)
                    .map(item -> new ResolvedMarketingObject(
                            item.objectId(),
                            item.objectType(),
                            item.resolvedLocale(),
                            item.fallbackApplied(),
                            item.publicationStatus(),
                            item.publishReady(),
                            item.payload() == null ? Map.of() : item.payload()
                    ))
                    .toList();
        } catch (RestClientResponseException exception) {
            throw new DecorationRemoteException("营销对象解析失败", exception);
        }
    }

    private record Request(String locale,
                           String resolveMode,
                           OwnerContext ownerContext,
                           List<ObjectRef> objectRefs) {
    }

    private record OwnerContext(String ownerType, String ownerId) {
    }

    private record ObjectRef(String objectType, String objectId) {
    }

    private record Response(String objectId,
                            String objectType,
                            String resolvedLocale,
                            boolean fallbackApplied,
                            String publicationStatus,
                            boolean publishReady,
                            Map<String, Object> payload) {
    }
}
