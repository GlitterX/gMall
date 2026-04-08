package com.gmall.mallbff.application;

import java.util.List;
import java.util.Map;

public interface MarketingProjectionGateway {

    ProjectionView getProjection(String projectionType,
                                 String ownerType,
                                 String ownerId,
                                 String terminalType,
                                 String locale,
                                 String pageContext);

    record ProjectionView(List<Map<String, Object>> items,
                          String resolvedLocale,
                          boolean fallbackApplied,
                          int cacheTtlSeconds) {
    }
}
