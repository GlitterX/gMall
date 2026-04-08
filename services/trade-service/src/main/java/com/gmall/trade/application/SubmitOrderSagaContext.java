package com.gmall.trade.application;

import java.util.List;

public record SubmitOrderSagaContext(String businessIdempotencyKey,
                                     String buyerId,
                                     List<String> orderIds) {
}
