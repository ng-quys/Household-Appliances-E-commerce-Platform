package com.example.WebBanDoGiaDung.dto;

import lombok.Data;

@Data
public class ProfileAddressForm {
    private Integer addressId;
    private String accountUsername;
    private String accountPhoneNumber;
    private Integer provinceId;
    private Integer districtId;
    private Integer wardId;
    private String content;
    private Boolean isDefault;
}
