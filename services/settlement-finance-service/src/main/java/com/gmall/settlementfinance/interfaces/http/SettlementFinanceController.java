package com.gmall.settlementfinance.interfaces.http;

import com.gmall.shared.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settlement")
public class SettlementFinanceController {

    @PostMapping("/entries/query")
    public ApiResponse<Map<String, Object>> queryEntries(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(
            Map.of(
                "request", request,
                "entries", List.of()
            ),
            null
        );
    }
}
