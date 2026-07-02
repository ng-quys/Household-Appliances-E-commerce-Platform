package com.example.WebBanDoGiaDung.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileAddressView {
    private Integer accountAddressId;
    private String accountUsername;
    private String accountPhoneNumber;
    private Integer provinceId;
    private Integer districtId;
    private Integer wardId;
    private String provinceName;
    private String districtName;
    private String wardName;
    private String content;
    private Boolean isDefault;
}