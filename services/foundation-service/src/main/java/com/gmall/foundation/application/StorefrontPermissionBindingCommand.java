package com.gmall.foundation.application;

public record StorefrontPermissionBindingCommand(
        String editorRoleIds,
        String submitterRoleIds,
        String operatorId,
        String operationReason
) {
}
