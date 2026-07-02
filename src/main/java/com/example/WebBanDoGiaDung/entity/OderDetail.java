package com.example.WebBanDoGiaDung.entity;

import com.example.WebBanDoGiaDung.entity.id.OderDetailId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Oder_Detail")
public class OderDetail extends Auditable{
    @EmbeddedId
    private OderDetailId id;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "status", length = 1)
    private String status;

    @Column(name = "transection", nullable = false, length = 50)
    private String transection;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "create_by", nullable = false, length = 20)
    private String createBy;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @Column(name = "update_by", nullable = false, length = 20)
    private String updateBy;

    @Column(name = "update_at", nullable = false)
    private LocalDateTime updateAt;

    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private OrderEntity order;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    private Product product;
}
