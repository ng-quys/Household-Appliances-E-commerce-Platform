package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.CartDto;
import jakarta.servlet.http.HttpSession;

public interface CartService {
    CartDto buildCart(HttpSession session);

    int getCartQuantity(HttpSession session);
}
