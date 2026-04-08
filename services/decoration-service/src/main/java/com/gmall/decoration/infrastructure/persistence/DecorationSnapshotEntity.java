package com.gmall.decoration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "decoration_snapshot")
public class DecorationSnapshotEntity {

    @Id
    private String snapshotId;

    @Column(nullable = false)
    private String pageId;

    @Column(nullable = false)
    private String terminalType;

    @Column(nullable = false)
    private int snapshotVersion;

    @Column(nullable = false)
    private String sourceDraftId;

    @Column(nullable = false, length = 16000)
    private String publishedPayload;

    @Column(nullable = false)
    private String payloadChecksum;

    @Column(nullable = false)
    private String snapshotStatus;

    @Column(nullable = false)
    private String publishedBy;

    @Column(nullable = false)
    private OffsetDateTime publishedAt;

    protected DecorationSnapshotEntity() {
    }

    public DecorationSnapshotEntity(String snapshotId,
                                    String pageId,
                                    String terminalType,
                                    int snapshotVersion,
                                    String sourceDraftId,
                                    String publishedPayload,
                                    String payloadChecksum,
                                    String snapshotStatus,
                                    String publishedBy,
                                    OffsetDateTime publishedAt) {
        this.snapshotId = snapshotId;
        this.pageId = pageId;
        this.terminalType = terminalType;
        this.snapshotVersion = snapshotVersion;
        this.sourceDraftId = sourceDraftId;
        this.publishedPayload = publishedPayload;
        this.payloadChecksum = payloadChecksum;
        this.snapshotStatus = snapshotStatus;
        this.publishedBy = publishedBy;
        this.publishedAt = publishedAt;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public String getPageId() {
        return pageId;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public int getSnapshotVersion() {
        return snapshotVersion;
    }

    public String getPublishedPayload() {
        return publishedPayload;
    }

    public String getPayloadChecksum() {
        return payloadChecksum;
    }

    public String getSnapshotStatus() {
        return snapshotStatus;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
