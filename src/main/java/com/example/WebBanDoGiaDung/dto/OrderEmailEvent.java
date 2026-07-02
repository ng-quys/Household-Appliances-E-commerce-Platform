package com.example.WebBanDoGiaDung.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEmailEvent implements Serializable {
    private String eventType;
    private Integer orderId;
    private String customerEmail;
    private String customerName;
    private String paymentMethod;
    private String orderStatus;
    private String total;
    private String orderDate;
}
