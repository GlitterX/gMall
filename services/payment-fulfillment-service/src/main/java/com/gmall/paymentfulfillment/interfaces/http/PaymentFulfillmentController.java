package com.gmall.paymentfulfillment.interfaces.http;

import com.gmall.shared.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class PaymentFulfillmentController {

    @PostMapping("/payment/orders/{orderId}/pay")
    public ApiResponse<Map<String, Object>> payOrder(@PathVariable String orderId,
                                                      @RequestBody Map<String, Object> request) {
        return ApiResponse.success(
            Map.of(
                "orderId", orderId,
                "paymentStatus", "PENDING",
                "request", request
            ),
            null
        );
    }

    @PostMapping("/payment/refunds")
    public ApiResponse<Map<String, Object>> refund(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(
            Map.of(
                "refundId", "refund-placeholder",
                "refundStatus", "PENDING",
                "request", request
            ),
            null
        );
    }
}
