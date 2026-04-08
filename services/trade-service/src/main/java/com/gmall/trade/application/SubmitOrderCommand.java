package com.gmall.trade.application;

import java.util.List;

public record SubmitOrderCommand(String businessIdempotencyKey,
                                 String buyerId,
                                 String locale,
                                 List<SubmitOrderItem> items) {
}
