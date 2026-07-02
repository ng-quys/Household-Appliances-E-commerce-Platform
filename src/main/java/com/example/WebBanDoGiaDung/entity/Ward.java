package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Wards")
public class Ward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ward_id")
    private Integer wardId;

    @Column(name = "district_id", nullable = false, insertable = false, updatable = false)
    private Integer districtId;

    @Column(name = "ward_name", nullable = false, length = 50)
    private String wardName;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", referencedColumnName = "district_id")
    private District district;

    @OneToMany(mappedBy = "ward")
    private List<AccountAddress> accountAddresses = new ArrayList<>();

    @OneToMany(mappedBy = "ward")
    private List<OrderAddress> orderAddresses = new ArrayList<>();
}
