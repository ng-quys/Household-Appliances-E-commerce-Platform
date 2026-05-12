package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "AccountAddress")
public class AccountAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_address_id")
    private Integer accountAddressId;

    @Column(name = "account_id", nullable = false, insertable = false, updatable = false)
    private Integer accountId;

    @Column(name = "province_id", nullable = false, insertable = false, updatable = false)
    private Integer provinceId;

    @Column(name = "district_id", nullable = false, insertable = false, updatable = false)
    private Integer districtId;

    @Column(name = "ward_id", nullable = false, insertable = false, updatable = false)
    private Integer wardId;

    @Column(name = "accountPhoneNumber", length = 10)
    private String accountPhoneNumber;

    @Column(name = "accountUsername", length = 20)
    private String accountUsername;

    @Column(name = "content", length = 50)
    private String content;

    @Column(name = "isDefault", nullable = false)
    private Boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", referencedColumnName = "province_id")
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", referencedColumnName = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", referencedColumnName = "ward_id")
    private Ward ward;
}
