package com.gmall.catalog.application;

public record UpdateMerchantOfferProductCommand(ProductContentDocument offerContent, String offerStatus) {
}
