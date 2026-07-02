package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Districts")
public class District {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "district_id")
    private Integer districtId;

    @Column(name = "province_id", nullable = false, insertable = false, updatable = false)
    private Integer provinceId;

    @Column(name = "district_name", nullable = false, length = 50)
    private String districtName;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", referencedColumnName = "province_id")
    private Province province;

    @OneToMany(mappedBy = "district")
    private List<Ward> wards = new ArrayList<>();

    @OneToMany(mappedBy = "district")
    private List<AccountAddress> accountAddresses = new ArrayList<>();

    @OneToMany(mappedBy = "district")
    private List<OrderAddress> orderAddresses = new ArrayList<>();
}
