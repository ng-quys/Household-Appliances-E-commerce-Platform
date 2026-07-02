package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Delivery")
public class Delivery extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Integer deliveryId;

    @Column(name = "delivery_name", nullable = false, length = 100)
    private String deliveryName;

    @Column(name = "price", precision = 19, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "status", length = 1)
    private String status;

    @OneToMany(mappedBy = "delivery")
    private List<OrderEntity> orders = new ArrayList<>();
}
