package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;

public interface OrderEmailService {
    void sendOrderEmail(OrderEmailEvent event);
}
