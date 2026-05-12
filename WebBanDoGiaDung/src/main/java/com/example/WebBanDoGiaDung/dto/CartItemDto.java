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
public class CartItemDto implements Serializable {
    private Integer productId;
    private String productName;
    private Double price;
    private Integer quantity;
    private String image;

    public double getLineTotal() {
        return (price == null ? 0D : price) * (quantity == null ? 0 : quantity);
    }
}
