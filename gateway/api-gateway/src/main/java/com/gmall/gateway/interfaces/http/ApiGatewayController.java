package com.gmall.gateway.interfaces.http;

import com.gmall.shared.api.ApiResponse;
import com.gmall.shared.api.ErrorCode;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping
public class ApiGatewayController {

    private final RestClient restClient;
    private final String mallBffBaseUrl;
    private final String adminBffBaseUrl;

    public ApiGatewayController(@Value("${gmall.mall-bff.base-url:http://127.0.0.1:18072}") String mallBffBaseUrl,
                                @Value("${gmall.admin-bff.base-url:http://127.0.0.1:18071}") String adminBffBaseUrl) {
        this.restClient = RestClient.builder().build();
        this.mallBffBaseUrl = mallBffBaseUrl;
        this.adminBffBaseUrl = adminBffBaseUrl;
    }

    @GetMapping("/api/mall/entries/resolve")
    public ResponseEntity<?> routeMallEntries(@RequestParam String entryType,
                                              @RequestParam(required = false) String storefrontId,
                                              @RequestParam(defaultValue = "MOBILE") String terminalType,
                                              @RequestParam(defaultValue = "zh-CN") String locale) {
        URI uri = UriComponentsBuilder.fromUriString(mallBffBaseUrl)
            .path("/mall/entries/resolve")
            .queryParam("entryType", entryType)
            .queryParam("storefrontId", storefrontId)
            .queryParam("terminalType", terminalType)
            .queryParam("locale", locale)
            .build(true)
            .toUri();
        return forwardGet(uri);
    }

    @GetMapping("/api/admin/workbench/summary")
    public ResponseEntity<?> routeAdminSummary() {
        URI uri = UriComponentsBuilder.fromUriString(adminBffBaseUrl)
            .path("/admin/workbench/summary")
            .build(true)
            .toUri();
        return forwardGet(uri);
    }

    @GetMapping("/api/mall/cart")
    public ResponseEntity<?> routeMallCart(@RequestParam(defaultValue = "zh-CN") String locale) {
        URI uri = UriComponentsBuilder.fromUriString(mallBffBaseUrl)
            .path("/mall/cart")
            .queryParam("locale", locale)
            .build(true)
            .toUri();
        return forwardGet(uri);
    }

    @GetMapping("/api/gateway/probe")
    public ApiResponse<Map<String, String>> probe() {
        return ApiResponse.success(
            Map.of(
                "status", "UP",
                "mallBffBaseUrl", mallBffBaseUrl,
                "adminBffBaseUrl", adminBffBaseUrl
            ),
            null
        );
    }

    private ResponseEntity<?> forwardGet(URI uri) {
        try {
            ResponseEntity<String> response = restClient.get().uri(uri).retrieve().toEntity(String.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception exception) {
            ApiResponse<Map<String, String>> fallback = ApiResponse.failure(
                ErrorCode.DOWNSTREAM_UNAVAILABLE,
                "downstream is unavailable"
            );
            return ResponseEntity.status(HttpStatusCode.valueOf(502)).body(fallback);
        }
    }
}
