package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "`Order`")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "orderAddressId", insertable = false, updatable = false)
    private Integer orderAddressId;

    @Column(name = "payment_id", nullable = false, insertable = false, updatable = false)
    private Integer paymentId;

    @Column(name = "delivery_id", nullable = false, insertable = false, updatable = false)
    private Integer deliveryId;

    @Column(name = "oder_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "account_id", nullable = false, insertable = false, updatable = false)
    private Integer accountId;

    @Column(name = "status", length = 1)
    private String status;

    @Column(name = "order_note", length = 200)
    private String orderNote;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @Column(name = "total", nullable = false)
    private Double total;

    @Column(name = "create_by", nullable = false, length = 100)
    private String createBy;

    @Column(name = "update_by", nullable = false, length = 100)
    private String updateBy;

    @Column(name = "update_at", nullable = false)
    private LocalDateTime updateAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", referencedColumnName = "delivery_id")
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", referencedColumnName = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderAddressId", referencedColumnName = "orderAddressId")
    private OrderAddress orderAddress;

    @OneToMany(mappedBy = "order")
    private List<OderDetail> oderDetails = new ArrayList<>();
}
