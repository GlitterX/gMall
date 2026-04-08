package com.gmall.decoration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "decoration_outbox_event")
public class DecorationOutboxEventEntity {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private long aggregateVersion;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, length = 8000)
    private String payload;

    @Column(nullable = false)
    private String publishStatus;

    @Column(nullable = false)
    private int retryCount;

    private OffsetDateTime nextRetryAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime publishedAt;

    protected DecorationOutboxEventEntity() {
    }

    public DecorationOutboxEventEntity(String eventId,
                                       String aggregateType,
                                       String aggregateId,
                                       long aggregateVersion,
                                       String eventType,
                                       String payload,
                                       String publishStatus,
                                       int retryCount,
                                       OffsetDateTime nextRetryAt,
                                       OffsetDateTime createdAt,
                                       OffsetDateTime publishedAt) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.eventType = eventType;
        this.payload = payload;
        this.publishStatus = publishStatus;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public String getPayload() {
        return payload;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void markPublished(OffsetDateTime publishedAt) {
        this.publishStatus = "SENT";
        this.publishedAt = publishedAt;
        this.nextRetryAt = null;
    }

    public void markRetry(OffsetDateTime nextRetryAt) {
        this.publishStatus = "FAILED";
        this.retryCount += 1;
        this.nextRetryAt = nextRetryAt;
    }
}
