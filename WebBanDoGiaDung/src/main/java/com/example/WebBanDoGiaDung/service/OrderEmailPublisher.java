package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;

    //đẩy OrderEmailEvent vào RabbitMQ
public interface OrderEmailPublisher {
    //báo hệ thống rằng vừa tạo đơn hàng, cần gửi email xác nhận đơn
    void publishOrderCreated(OrderEmailEvent event);

    // báo hệ thống rằng đơn hàng đã thanh toán, cần gửi email thanh toán/thành công
    void publishOrderPaid(OrderEmailEvent event);
}
