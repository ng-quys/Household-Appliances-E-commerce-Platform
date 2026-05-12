package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.controller.CartController;
import com.example.WebBanDoGiaDung.dto.CartDto;
import com.example.WebBanDoGiaDung.dto.CartItemDto;
import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.service.CartService;
import com.example.WebBanDoGiaDung.service.ProductService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
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
                .mapToInt(Integer::intValue)
                .sum();
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getCartMap(HttpSession session) {
        Object cart = session.getAttribute(CartController.CART_SESSION_KEY);
        if (cart instanceof Map<?, ?> existing) {
            return (Map<Integer, Integer>) existing;
        }
        return Map.of();
    }
}
