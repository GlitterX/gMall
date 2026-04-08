package com.gmall.catalog.application;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productViewId) {
        super("商品投影不存在: " + productViewId);
    }
}
