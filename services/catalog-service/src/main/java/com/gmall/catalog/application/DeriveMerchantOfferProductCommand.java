package com.gmall.catalog.application;

public record DeriveMerchantOfferProductCommand(String merchantId,
                                                String merchantOrganizationId,
                                                String relationId,
                                                ProductContentDocument offerContent) {
}
