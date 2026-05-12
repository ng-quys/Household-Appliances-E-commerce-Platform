package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "OrderAddress")
public class OrderAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderAddressId")
    private Integer orderAddressId;

    @Column(name = "province_id", insertable = false, updatable = false)
    private Integer provinceId;

    @Column(name = "district_id", insertable = false, updatable = false)
    private Integer districtId;

    @Column(name = "ward_id", insertable = false, updatable = false)
    private Integer wardId;

    @Column(name = "orderPhonenumber", length = 10)
    private String orderPhoneNumber;

    @Column(name = "orderUsername", length = 20)
    private String orderUsername;

    @Column(name = "content", length = 150)
    private String content;

    @Column(name = "timesEdit", nullable = false)
    private Integer timesEdit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", referencedColumnName = "province_id")
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", referencedColumnName = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", referencedColumnName = "ward_id")
    private Ward ward;

    @OneToMany(mappedBy = "orderAddress")
    private List<OrderEntity> orders = new ArrayList<>();
}
