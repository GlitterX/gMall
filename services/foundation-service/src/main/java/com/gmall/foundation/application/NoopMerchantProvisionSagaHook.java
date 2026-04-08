package com.gmall.foundation.application;

import org.springframework.stereotype.Component;

@Component
class NoopMerchantProvisionSagaHook implements MerchantProvisionSagaHook {

    @Override
    public void afterStep(MerchantProvisionStep step, MerchantProvisionSagaContext context) {
        // 默认不插入任何故障点，测试可通过 MockBean 覆盖。
    }
}
