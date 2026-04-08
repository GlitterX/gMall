package com.gmall.foundation.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AuthorizedCatalogScope {

    private final boolean catalogAll;
    private final Set<String> categoryIds;
    private final Set<String> productIds;

    private AuthorizedCatalogScope(boolean catalogAll, Set<String> categoryIds, Set<String> productIds) {
        this.catalogAll = catalogAll;
        this.categoryIds = Collections.unmodifiableSet(new LinkedHashSet<>(categoryIds));
        this.productIds = Collections.unmodifiableSet(new LinkedHashSet<>(productIds));
    }

    public static AuthorizedCatalogScope parse(String rawScope) {
        if (!hasText(rawScope)) {
            throw new IllegalArgumentException("授权范围不能为空");
        }
        String normalizedInput = rawScope.trim();
        if ("ALL".equalsIgnoreCase(normalizedInput) || "CATALOG:ALL".equalsIgnoreCase(normalizedInput)) {
            return new AuthorizedCatalogScope(true, Set.of(), Set.of());
        }

        boolean catalogAll = false;
        LinkedHashSet<String> categoryIds = new LinkedHashSet<>();
        LinkedHashSet<String> productIds = new LinkedHashSet<>();
        String[] segments = normalizedInput.split(";");
        for (String segment : segments) {
            if (!hasText(segment)) {
                continue;
            }
            int separatorIndex = segment.indexOf(':');
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("授权范围格式非法: " + rawScope);
            }
            String scopeType = segment.substring(0, separatorIndex).trim().toUpperCase(Locale.ROOT);
            String scopeValue = segment.substring(separatorIndex + 1).trim();
            switch (scopeType) {
                case "CATALOG" -> {
                    if (!"ALL".equalsIgnoreCase(scopeValue)) {
                        throw new IllegalArgumentException("CATALOG 只支持 ALL");
                    }
                    catalogAll = true;
                }
                case "CATEGORY" -> addScopeValues(scopeType, scopeValue, categoryIds);
                case "PRODUCT" -> addScopeValues(scopeType, scopeValue, productIds);
                default -> throw new IllegalArgumentException("不支持的授权范围类型: " + scopeType);
            }
        }
        if (!catalogAll && categoryIds.isEmpty() && productIds.isEmpty()) {
            throw new IllegalArgumentException("授权范围不能为空");
        }
        return new AuthorizedCatalogScope(catalogAll, categoryIds, productIds);
    }

    public String normalizedValue() {
        List<String> segments = new ArrayList<>();
        if (catalogAll) {
            segments.add("CATALOG:ALL");
        }
        if (!categoryIds.isEmpty()) {
            segments.add("CATEGORY:" + String.join(",", categoryIds));
        }
        if (!productIds.isEmpty()) {
            segments.add("PRODUCT:" + String.join(",", productIds));
        }
        return String.join(";", segments);
    }

    public CatalogAuthorizationDecision authorize(String categoryId, String productId) {
        boolean hasCategoryId = hasText(categoryId);
        boolean hasProductId = hasText(productId);
        if (!hasCategoryId && !hasProductId) {
            return CatalogAuthorizationDecision.notEvaluated("TARGET_NOT_PROVIDED");
        }
        if (catalogAll) {
            return CatalogAuthorizationDecision.authorized("CATALOG", "ALL", "CATALOG_ALL_AUTHORIZED");
        }
        if (hasProductId) {
            String normalizedProductId = productId.trim();
            if (productIds.contains(normalizedProductId)) {
                return CatalogAuthorizationDecision.authorized("PRODUCT", normalizedProductId, "PRODUCT_AUTHORIZED");
            }
        }
        if (hasCategoryId) {
            String normalizedCategoryId = categoryId.trim();
            if (categoryIds.contains(normalizedCategoryId)) {
                return CatalogAuthorizationDecision.authorized("CATEGORY", normalizedCategoryId, "CATEGORY_AUTHORIZED");
            }
        }
        if (hasProductId && !hasCategoryId && !categoryIds.isEmpty()) {
            return CatalogAuthorizationDecision.rejected("CATEGORY_CONTEXT_REQUIRED");
        }
        if (hasProductId && hasCategoryId) {
            return CatalogAuthorizationDecision.rejected("PRODUCT_AND_CATEGORY_NOT_AUTHORIZED");
        }
        if (hasProductId) {
            return CatalogAuthorizationDecision.rejected("PRODUCT_NOT_AUTHORIZED");
        }
        return CatalogAuthorizationDecision.rejected("CATEGORY_NOT_AUTHORIZED");
    }

    private static void addScopeValues(String scopeType, String scopeValue, LinkedHashSet<String> target) {
        if (!hasText(scopeValue)) {
            throw new IllegalArgumentException(scopeType + " 范围不能为空");
        }
        String[] values = scopeValue.split(",");
        for (String value : values) {
            if (!hasText(value)) {
                throw new IllegalArgumentException(scopeType + " 范围存在空值");
            }
            target.add(value.trim());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
