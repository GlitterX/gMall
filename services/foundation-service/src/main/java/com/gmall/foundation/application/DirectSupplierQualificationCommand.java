package com.gmall.foundation.application;

public record DirectSupplierQualificationCommand(
        String qualificationId,
        String organizationId,
        String operatorId,
        String operationReason
) {
}
