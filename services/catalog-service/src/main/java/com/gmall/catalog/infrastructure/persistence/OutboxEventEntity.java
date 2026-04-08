package com.gmall.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "catalog_outbox_event")
public class OutboxEventEntity {

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

    @Column(nullable = false)
    private OffsetDateTime lastAttemptAt;

    @Column(length = 1024)
    private String lastErrorMessage;

    private OffsetDateTime deadLetteredAt;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(String eventId,
                             String aggregateType,
                             String aggregateId,
                             long aggregateVersion,
                             String eventType,
                             String payload,
                             String publishStatus,
                             int retryCount,
                             OffsetDateTime nextRetryAt,
                             OffsetDateTime createdAt,
                             OffsetDateTime publishedAt,
                             OffsetDateTime lastAttemptAt,
                             String lastErrorMessage,
                             OffsetDateTime deadLetteredAt) {
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
        this.lastAttemptAt = lastAttemptAt;
        this.lastErrorMessage = lastErrorMessage;
        this.deadLetteredAt = deadLetteredAt;
    }

    public String getPayload() {
        return payload;
    }

    public String getEventId() {
        return eventId;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public OffsetDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public OffsetDateTime getDeadLetteredAt() {
        return deadLetteredAt;
    }

    public void markPublished(OffsetDateTime publishedAt) {
        this.publishStatus = "SENT";
        this.publishedAt = publishedAt;
        this.nextRetryAt = null;
        this.lastAttemptAt = publishedAt;
        this.lastErrorMessage = null;
        this.deadLetteredAt = null;
    }

    public void markRetry(OffsetDateTime attemptedAt,
                          OffsetDateTime nextRetryAt,
                          int maxRetryCount,
                          String errorMessage) {
        this.retryCount += 1;
        this.lastAttemptAt = attemptedAt;
        this.lastErrorMessage = trimErrorMessage(errorMessage);
        if (this.retryCount >= maxRetryCount) {
            this.publishStatus = "DEAD_LETTER";
            this.nextRetryAt = null;
            this.deadLetteredAt = attemptedAt;
            return;
        }
        this.publishStatus = "FAILED";
        this.nextRetryAt = nextRetryAt;
        this.deadLetteredAt = null;
    }

    private String trimErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        if (errorMessage.length() <= 1024) {
            return errorMessage;
        }
        return errorMessage.substring(0, 1024);
    }
}
