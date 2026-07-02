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
@Table(name = "Discount")
public class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disscount_id")
    private Integer disscountId;

    @Column(name = "discount_name", nullable = false, length = 100)
    private String discountName;

    @Column(name = "discount_star", nullable = false)
    private LocalDateTime discountStart;

    @Column(name = "discount_end", nullable = false)
    private LocalDateTime discountEnd;

    @Column(name = "discount_price", nullable = false)
    private Double discountPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "discount_code", length = 10)
    private String discountCode;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @Column(name = "create_by", nullable = false, length = 100)
    private String createBy;

    @Column(name = "update_by", nullable = false, length = 100)
    private String updateBy;

    @Column(name = "update_at", nullable = false)
    private LocalDateTime updateAt;

    @OneToMany(mappedBy = "discount")
    private List<Product> products = new ArrayList<>();
}
