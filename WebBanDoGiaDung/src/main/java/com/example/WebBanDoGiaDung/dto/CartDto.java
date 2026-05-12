package com.example.WebBanDoGiaDung.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto implements Serializable {
    private List<CartItemDto> items;
    private Integer totalQuantity;
    private Double subtotal;

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
