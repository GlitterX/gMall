package com.gmall.mallbff.infrastructure.remote;

import com.gmall.mallbff.application.DecorationSnapshotGateway;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DecorationSnapshotRestClient implements DecorationSnapshotGateway {

    private final RestClient restClient;

    @Autowired
    public DecorationSnapshotRestClient(@Value("${gmall.decoration-service.base-url}") String decorationBaseUrl) {
        this(RestClient.builder().baseUrl(decorationBaseUrl).build());
    }

    DecorationSnapshotRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PublishedSnapshotView getSnapshot(String storefrontId, String pageId, String terminalType) {
        try {
            Response response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/storefronts/{storefrontId}/decorations/pages/{pageId}/snapshot")
                            .queryParam("terminalType", terminalType)
                            .build(storefrontId, pageId)
                    )
                    .retrieve()
                    .body(Response.class);
            if (response == null) {
                throw new MallBffRemoteException("装修快照响应为空", null);
            }
            return new PublishedSnapshotView(
                    response.pageId(),
                    response.storefrontId(),
                    response.pageType(),
                    response.terminalType(),
                    response.snapshotId(),
                    response.snapshotVersion(),
                    response.publishedPayload() == null ? Map.of() : response.publishedPayload(),
                    response.publishedAt()
            );
        } catch (RestClientResponseException exception) {
            throw new MallBffRemoteException("读取装修快照失败", exception);
        }
    }

    private record Response(String pageId,
                            String storefrontId,
                            String pageType,
                            String terminalType,
                            String snapshotId,
                            int snapshotVersion,
                            Map<String, Object> publishedPayload,
                            java.time.OffsetDateTime publishedAt) {
    }
}
