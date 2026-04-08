package com.gmall.decoration.infrastructure.remote;

import com.gmall.decoration.application.CatalogDecorationProjectionGateway;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class CatalogDecorationProjectionRestClient implements CatalogDecorationProjectionGateway {

    private final RestClient restClient;

    @Autowired
    public CatalogDecorationProjectionRestClient(RestClient.Builder restClientBuilder,
                                                 @Value("${gmall.catalog-service.base-url}") String catalogBaseUrl) {
        this(restClientBuilder.baseUrl(catalogBaseUrl).build());
    }

    CatalogDecorationProjectionRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<DecorationProduct> getProducts(List<String> productViewIds, String locale) {
        try {
            Response[] response = restClient.post()
                    .uri("/internal/projections/decoration")
                    .body(new Request(productViewIds, locale))
                    .retrieve()
                    .body(Response[].class);
            if (response == null) {
                return List.of();
            }
            return java.util.Arrays.stream(response)
                    .map(item -> new DecorationProduct(
                            item.productViewId(),
                            item.title(),
                            item.coverImage(),
                            item.productStatus(),
                            item.sourceType(),
                            item.resolvedLocale(),
                            item.fallbackApplied(),
                            item.contentCompleteness()
                    ))
                    .toList();
        } catch (HttpClientErrorException exception) {
            return List.of();
        }
    }

    private record Request(List<String> productViewIds, String locale) {
    }

    private record Response(String productViewId,
                            String title,
                            String coverImage,
                            String productStatus,
                            String sourceType,
                            String resolvedLocale,
                            boolean fallbackApplied,
                            String contentCompleteness) {
    }
}
