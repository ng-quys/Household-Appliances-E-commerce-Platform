package com.example.WebBanDoGiaDung.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryPredictionDto {
    private Integer productId;
    private String productName;
    private Long currentStock;
    private Long soldLast30Days;
    private Long predictedDemandNext30Days;
    private Long suggestedImportQuantity;
    private String warningLevel;
    private Double averageDailySales;
    private Long estimatedDaysRemaining;
}
