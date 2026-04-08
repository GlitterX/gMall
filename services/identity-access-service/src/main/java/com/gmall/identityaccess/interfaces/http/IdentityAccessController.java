package com.gmall.identityaccess.interfaces.http;

import com.gmall.shared.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identity")
public class IdentityAccessController {

    @PostMapping("/auth/admin/login")
    public ApiResponse<Map<String, Object>> adminLogin(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(
            Map.of(
                "sessionToken", "admin-token-placeholder",
                "accountType", "ADMIN",
                "request", request
            ),
            null
        );
    }

    @PostMapping("/auth/consumer/login")
    public ApiResponse<Map<String, Object>> consumerLogin(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(
            Map.of(
                "sessionToken", "consumer-token-placeholder",
                "accountType", "CONSUMER",
                "request", request
            ),
            null
        );
    }

    @GetMapping("/addresses")
    public ApiResponse<List<Map<String, Object>>> listAddresses() {
        return ApiResponse.success(
            List.of(
                Map.of(
                    "consumerAddressId", "addr-001",
                    "receiverName", "placeholder",
                    "isDefault", true
                )
            ),
            null
        );
    }
}
