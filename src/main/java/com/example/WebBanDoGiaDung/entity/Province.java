package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Provinces")
public class Province {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "province_id")
    private Integer provinceId;

    @Column(name = "province_name", nullable = false, length = 50)
    private String provinceName;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @OneToMany(mappedBy = "province")
    private List<District> districts = new ArrayList<>();

    @OneToMany(mappedBy = "province")
    private List<AccountAddress> accountAddresses = new ArrayList<>();

    @OneToMany(mappedBy = "province")
    private List<OrderAddress> orderAddresses = new ArrayList<>();
}
