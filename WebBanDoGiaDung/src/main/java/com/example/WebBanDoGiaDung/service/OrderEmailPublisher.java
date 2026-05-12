package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;

public interface OrderEmailPublisher {
    void publishOrderCreated(OrderEmailEvent event);

    void publishOrderPaid(OrderEmailEvent event);
}
