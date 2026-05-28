package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;

public interface OrderEmailService {
    // chịu trách nhiệm tạo nội dung email và gửi mail
    void sendOrderEmail(OrderEmailEvent event);
}
