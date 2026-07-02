package com.example.WebBanDoGiaDung.listener;

import com.example.WebBanDoGiaDung.config.RabbitMqConfig;
import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;
import com.example.WebBanDoGiaDung.service.OrderEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEmailListener {

    private final OrderEmailService orderEmailService;

    @RabbitListener(queues = RabbitMqConfig.ORDER_EMAIL_QUEUE)
    //bắt sự kiện gửi mail,  nghe queue từ Publisher đẩy OrderEmailEvent vào RabbitMQ
    public void handleOrderEmailEvent(OrderEmailEvent event) {
        try {
            log.info("Listener received order email event. queue={}, orderId={}, eventType={}",
                    RabbitMqConfig.ORDER_EMAIL_QUEUE,
                    event != null ? event.getOrderId() : null,
                    event != null ? event.getEventType() : null);
            orderEmailService.sendOrderEmail(event);
        } catch (Exception exception) {
            log.error("Unexpected error while handling order email event. orderId={}, eventType={}",
                    event != null ? event.getOrderId() : null,
                    event != null ? event.getEventType() : null,
                    exception);
        }
    }
}
