package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.OrderEntity;
import jakarta.servlet.http.HttpSession;

public interface CheckoutService {
    OrderEntity createCodOrder(Integer accountId, Integer deliveryId, HttpSession session);

    OrderEntity createBankTransferOrder(Integer accountId, Integer deliveryId, HttpSession session);
}
