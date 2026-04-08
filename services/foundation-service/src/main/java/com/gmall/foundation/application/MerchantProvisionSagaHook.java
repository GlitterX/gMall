package com.gmall.foundation.application;

public interface MerchantProvisionSagaHook {

    void afterStep(MerchantProvisionStep step, MerchantProvisionSagaContext context);
}
