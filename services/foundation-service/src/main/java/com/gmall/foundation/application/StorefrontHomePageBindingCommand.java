package com.gmall.foundation.application;

public record StorefrontHomePageBindingCommand(
        String homePageId,
        String operatorId,
        String operationReason
) {
}
