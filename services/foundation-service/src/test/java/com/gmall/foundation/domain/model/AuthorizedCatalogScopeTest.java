package com.gmall.foundation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuthorizedCatalogScopeTest {

    @Test
    void parseNormalizesLegacyAndCompositeScopes() {
        assertThat(AuthorizedCatalogScope.parse("ALL").normalizedValue()).isEqualTo("CATALOG:ALL");
        assertThat(AuthorizedCatalogScope.parse("CATEGORY:cat-1, cat-2;PRODUCT:prod-1").normalizedValue())
                .isEqualTo("CATEGORY:cat-1,cat-2;PRODUCT:prod-1");
    }

    @Test
    void authorizeMatchesProductThenCategoryThenCatalogAll() {
        AuthorizedCatalogScope scope = AuthorizedCatalogScope.parse("CATEGORY:cat-1;PRODUCT:prod-1");

        CatalogAuthorizationDecision productDecision = scope.authorize("cat-9", "prod-1");
        CatalogAuthorizationDecision categoryDecision = scope.authorize("cat-1", "prod-9");
        CatalogAuthorizationDecision allDecision = AuthorizedCatalogScope.parse("ALL").authorize(null, "prod-9");

        assertThat(productDecision.catalogAuthorized()).isTrue();
        assertThat(productDecision.matchedScopeType()).isEqualTo("PRODUCT");
        assertThat(productDecision.matchedScopeValue()).isEqualTo("prod-1");
        assertThat(productDecision.authorizationReason()).isEqualTo("PRODUCT_AUTHORIZED");

        assertThat(categoryDecision.catalogAuthorized()).isTrue();
        assertThat(categoryDecision.matchedScopeType()).isEqualTo("CATEGORY");
        assertThat(categoryDecision.matchedScopeValue()).isEqualTo("cat-1");
        assertThat(categoryDecision.authorizationReason()).isEqualTo("CATEGORY_AUTHORIZED");

        assertThat(allDecision.catalogAuthorized()).isTrue();
        assertThat(allDecision.matchedScopeType()).isEqualTo("CATALOG");
        assertThat(allDecision.matchedScopeValue()).isEqualTo("ALL");
        assertThat(allDecision.authorizationReason()).isEqualTo("CATALOG_ALL_AUTHORIZED");
    }

    @Test
    void authorizeRejectsWhenCategoryContextMissingOrNoMatch() {
        AuthorizedCatalogScope scope = AuthorizedCatalogScope.parse("CATEGORY:cat-1");

        CatalogAuthorizationDecision missingCategoryDecision = scope.authorize(null, "prod-1");
        CatalogAuthorizationDecision noMatchDecision = scope.authorize("cat-9", null);

        assertThat(missingCategoryDecision.catalogAuthorized()).isFalse();
        assertThat(missingCategoryDecision.matchedScopeType()).isEqualTo("NONE");
        assertThat(missingCategoryDecision.authorizationReason()).isEqualTo("CATEGORY_CONTEXT_REQUIRED");

        assertThat(noMatchDecision.catalogAuthorized()).isFalse();
        assertThat(noMatchDecision.matchedScopeType()).isEqualTo("NONE");
        assertThat(noMatchDecision.authorizationReason()).isEqualTo("CATEGORY_NOT_AUTHORIZED");
    }

    @Test
    void parseRejectsUnsupportedScopeType() {
        assertThatThrownBy(() -> AuthorizedCatalogScope.parse("BRAND:brand-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的授权范围类型");
    }
}
