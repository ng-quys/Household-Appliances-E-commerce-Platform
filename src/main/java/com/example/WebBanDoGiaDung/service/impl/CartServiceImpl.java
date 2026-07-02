package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.controller.CartController;
import com.example.WebBanDoGiaDung.dto.CartDto;
import com.example.WebBanDoGiaDung.dto.CartItemDto;
import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.service.CartService;
import com.example.WebBanDoGiaDung.service.ProductService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final ProductService productService;

    @Override
    public CartDto buildCart(HttpSession session) {
        Map<Integer, Integer> cartMap = getCartMap(session);
        List<CartItemDto> items = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : cartMap.entrySet()) {
            Optional<ProductCacheDto> productOptional = productService.findProductDetailById(entry.getKey());
            if (productOptional.isEmpty()) {
                continue;
            }
            ProductCacheDto product = productOptional.get();
            items.add(CartItemDto.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(entry.getValue())
                    .image(product.getImage())
                    .build());
        }

        int totalQuantity = items.stream()
                .map(CartItemDto::getQuantity)
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();

        double subtotal = items.stream()
                .mapToDouble(CartItemDto::getLineTotal)
                .sum();

        return CartDto.builder()
                .items(items)
                .totalQuantity(totalQuantity)
                .subtotal(subtotal)
                .build();
    }

    @Override
    public int getCartQuantity(HttpSession session) {
        return getCartMap(session).values().stream()
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private Map<Integer, Integer> getCartMap(HttpSession session) {
        Object cart = session.getAttribute(CartController.CART_SESSION_KEY);
        Map<Integer, Integer> normalized = new LinkedHashMap<>();

        if (cart instanceof Map<?, ?> existing) {
            existing.forEach((key, value) -> {
                Integer productId = toPositiveInteger(key);
                Integer quantity = toPositiveInteger(value);
                if (productId != null && quantity != null) {
                    normalized.put(productId, quantity);
                }
            });
        }

        session.setAttribute(CartController.CART_SESSION_KEY, normalized);
        return normalized;
    }

    private Integer toPositiveInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Integer converted = null;
            if (value instanceof Integer integerValue) {
                converted = integerValue;
            } else if (value instanceof Number numberValue) {
                converted = numberValue.intValue();
            } else if (value instanceof String stringValue && !stringValue.isBlank()) {
                converted = Integer.valueOf(stringValue.trim());
            }
            return converted != null && converted > 0 ? converted : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
