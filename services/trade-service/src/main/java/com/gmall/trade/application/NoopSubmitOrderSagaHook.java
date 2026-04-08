package com.gmall.trade.application;

import org.springframework.stereotype.Component;

@Component
public class NoopSubmitOrderSagaHook implements SubmitOrderSagaHook {

    @Override
    public void afterStep(SubmitOrderSagaStep step, SubmitOrderSagaContext context) {
        // 测试之外不做额外动作，保留给后续 Saga 扩展。
    }
}
