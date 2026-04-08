package com.gmall.pricinginventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_reservation")
public class InventoryReservationEntity {

    @Id
    private String reservationId;

    @Column(nullable = false, unique = true)
    private String businessKey;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, length = 4000)
    private String reservationPayload;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected InventoryReservationEntity() {
    }

    public InventoryReservationEntity(String reservationId,
                                      String businessKey,
                                      String status,
                                      String reservationPayload,
                                      OffsetDateTime createdAt) {
        this.reservationId = reservationId;
        this.businessKey = businessKey;
        this.status = status;
        this.reservationPayload = reservationPayload;
        this.createdAt = createdAt;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getStatus() {
        return status;
    }

    public String getReservationPayload() {
        return reservationPayload;
    }

    public void markReleased() {
        status = "RELEASED";
    }

    public void markConfirmed() {
        status = "CONFIRMED";
    }
}
