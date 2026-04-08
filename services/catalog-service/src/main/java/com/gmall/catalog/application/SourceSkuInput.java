package com.gmall.catalog.application;

import java.util.Map;

public record SourceSkuInput(String specSignature, Map<String, String> specPayload) {
}
