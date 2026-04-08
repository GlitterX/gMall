package com.gmall.foundation.application;

public record StorefrontTerminalCommand(
        boolean enabled,
        String operatorId,
        String operationReason
) {
}
