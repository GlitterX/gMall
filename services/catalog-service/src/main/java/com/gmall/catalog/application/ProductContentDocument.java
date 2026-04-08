package com.gmall.catalog.application;

public record ProductContentDocument(LocalizedTextDocument productName, String coverImage) {

    public ProductContentDocument mergeOverride(ProductContentDocument override) {
        if (override == null) {
            return this;
        }
        LocalizedTextDocument mergedProductName = productName == null
                ? override.productName()
                : productName.mergeOverride(override.productName());
        String mergedCoverImage = hasText(override.coverImage()) ? override.coverImage() : coverImage;
        return new ProductContentDocument(mergedProductName, mergedCoverImage);
    }

    public String defaultLocale() {
        return productName == null ? null : productName.defaultLocale();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
