package com.gmall.decoration.application;

import java.util.List;
import java.util.Map;

public interface MarketingContentGateway {

    List<ResolvedMarketingObject> resolveObjects(List<MarketingObjectRef> objectRefs,
                                                 String locale,
                                                 String resolveMode,
                                                 String ownerType,
                                                 String ownerId);

    record MarketingObjectRef(String objectType, String objectId) {
    }

    record ResolvedMarketingObject(String objectId,
                                   String objectType,
                                   String resolvedLocale,
                                   boolean fallbackApplied,
                                   String publicationStatus,
                                   boolean publishReady,
                                   Map<String, Object> payload) {
    }
}
