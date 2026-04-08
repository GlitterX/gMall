package com.gmall.catalog.application;

public record UpdateSourceProductCommand(ProductContentDocument productContent, String productStatus) {
}
