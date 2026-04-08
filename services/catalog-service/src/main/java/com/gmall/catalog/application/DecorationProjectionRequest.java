package com.gmall.catalog.application;

import java.util.List;

public record DecorationProjectionRequest(List<String> productViewIds, String locale) {
}
