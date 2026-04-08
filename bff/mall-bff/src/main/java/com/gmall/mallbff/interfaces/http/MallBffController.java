package com.gmall.mallbff.interfaces.http;

import com.gmall.mallbff.application.MallPageRenderService;
import com.gmall.shared.api.ApiResponse;
import com.gmall.shared.localization.LocaleMeta;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MallBffController {

    private final MallPageRenderService mallPageRenderService;

    public MallBffController(MallPageRenderService mallPageRenderService) {
        this.mallPageRenderService = mallPageRenderService;
    }

    @GetMapping("/mall/entries/resolve")
    public ApiResponse<Map<String, Object>> resolveEntry(@RequestParam String entryType,
                                                         @RequestParam(required = false) String storefrontId,
                                                         @RequestParam(defaultValue = "MOBILE") String terminalType,
                                                         @RequestParam(defaultValue = "zh-CN") String locale) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entryType", entryType);
        payload.put("storefrontId", storefrontId == null ? "platform-default" : storefrontId);
        payload.put("terminalType", terminalType);
        payload.put("entryStatus", "OPEN");
        payload.put("homePageId", "home-default");
        return ApiResponse.success(payload, localeMeta(locale, locale, false));
    }

    @GetMapping("/mall/pages/render")
    public ApiResponse<Map<String, Object>> renderPage(@RequestParam String storefrontId,
                                                        @RequestParam String pageId,
                                                        @RequestParam(defaultValue = "MOBILE") String terminalType,
                                                        @RequestParam(defaultValue = "zh-CN") String locale) {
        Map<String, Object> payload = mallPageRenderService.renderPage(storefrontId, pageId, terminalType, locale);
        String resolvedLocale = payload.get("resolvedLocale") instanceof String value ? value : locale;
        boolean fallbackApplied = payload.get("fallbackApplied") instanceof Boolean value && value;
        return ApiResponse.success(payload, localeMeta(locale, resolvedLocale, fallbackApplied));
    }

    @GetMapping("/mall/products/{productViewId}")
    public ApiResponse<Map<String, Object>> getProduct(@PathVariable String productViewId,
                                                        @RequestParam String storefrontId,
                                                        @RequestParam(defaultValue = "zh-CN") String locale) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productViewId", productViewId);
        payload.put("storefrontId", storefrontId);
        payload.put("productName", "placeholder-product");
        payload.put("price", "0.00");
        return ApiResponse.success(payload, localeMeta(locale, locale, false));
    }

    @GetMapping("/mall/cart")
    public ApiResponse<Map<String, Object>> getCart(@RequestParam(defaultValue = "zh-CN") String locale) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", List.of());
        payload.put("totalAmount", "0.00");
        return ApiResponse.success(payload, localeMeta(locale, locale, false));
    }

    @PostMapping("/mall/checkout/preview")
    public ApiResponse<Map<String, Object>> previewCheckout(@Valid @RequestBody Map<String, Object> request,
                                                             @RequestParam(defaultValue = "zh-CN") String locale) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requested", request);
        payload.put("groups", List.of());
        payload.put("checkoutAllowed", true);
        return ApiResponse.success(payload, localeMeta(locale, locale, false));
    }

    private LocaleMeta localeMeta(String requestedLocale, String resolvedLocale, boolean fallbackApplied) {
        return new LocaleMeta(requestedLocale, resolvedLocale, fallbackApplied);
    }
}
