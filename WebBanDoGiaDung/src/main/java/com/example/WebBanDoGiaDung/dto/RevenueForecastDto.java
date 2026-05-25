package com.example.WebBanDoGiaDung.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueForecastDto {
    private String month;
    private Double actualRevenue;
    private Double forecastRevenue;
}