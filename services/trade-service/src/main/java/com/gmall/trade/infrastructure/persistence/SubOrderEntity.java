package com.gmall.trade.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trade_sub_order")
public class SubOrderEntity {

    @Id
    private String subOrderId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String storefrontId;

    @Column(nullable = false, length = 4000)
    private String orderItemSnapshot;

    protected SubOrderEntity() {
    }

    public SubOrderEntity(String subOrderId, String orderId, String sellerId, String storefrontId,
                          String orderItemSnapshot) {
        this.subOrderId = subOrderId;
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.storefrontId = storefrontId;
        this.orderItemSnapshot = orderItemSnapshot;
    }
}
