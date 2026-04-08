package com.gmall.mallbff.application;

import java.time.OffsetDateTime;
import java.util.Map;

public interface DecorationSnapshotGateway {

    PublishedSnapshotView getSnapshot(String storefrontId, String pageId, String terminalType);

    record PublishedSnapshotView(String pageId,
                                 String storefrontId,
                                 String pageType,
                                 String terminalType,
                                 String snapshotId,
                                 int snapshotVersion,
                                 Map<String, Object> publishedPayload,
                                 OffsetDateTime publishedAt) {
    }
}
