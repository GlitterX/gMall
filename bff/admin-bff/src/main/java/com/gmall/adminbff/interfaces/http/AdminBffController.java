package com.gmall.adminbff.interfaces.http;

import com.gmall.shared.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AdminBffController {

    @GetMapping("/admin/workbench/summary")
    public ApiResponse<Map<String, Object>> getWorkbenchSummary() {
        return ApiResponse.success(
            Map.of(
                "pendingReviewCount", 0,
                "slaWarningCount", 0,
                "module", "admin-bff"
            ),
            null
        );
    }

    @GetMapping("/admin/modules")
    public ApiResponse<List<String>> getModules() {
        return ApiResponse.success(
            List.of("foundation", "catalog", "pricing-inventory", "trade", "decoration"),
            null
        );
    }
}
