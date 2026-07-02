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
@Table(name = "Payment")
public class Payment extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "payment_name", nullable = false, length = 50)
    private String paymentName;


    @Column(name = "status", length = 1)
    private String status;



    @OneToMany(mappedBy = "payment")
    private List<OrderEntity> orders = new ArrayList<>();
}
