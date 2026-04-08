package com.gmall.foundation.application;

public interface DecorationSnapshotGateway {

    HomePageSnapshotVerification verifyHomePageSnapshot(String storefrontId, String pageId, String terminalType);

    record HomePageSnapshotVerification(HomePageValidationStatus validationStatus,
                                        String verificationStatus,
                                        String snapshotId,
                                        Integer snapshotVersion,
                                        String payloadChecksum,
                                        boolean checksumMatched) {
    }
}
