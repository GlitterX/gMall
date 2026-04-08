package com.gmall.trade.application;

public interface SubmitOrderSagaHook {

    void afterStep(SubmitOrderSagaStep step, SubmitOrderSagaContext context);
}
