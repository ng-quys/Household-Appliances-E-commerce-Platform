package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.config.RabbitMqConfig;
import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;
import com.example.WebBanDoGiaDung.service.OrderEmailPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailPublisherImpl implements OrderEmailPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrderCreated(OrderEmailEvent event) {
        publish(event, RabbitMqConfig.ROUTING_KEY_ORDER_CREATED);
    }

    @Override
    public void publishOrderPaid(OrderEmailEvent event) {
        publish(event, RabbitMqConfig.ROUTING_KEY_ORDER_PAID);
    }

    private void publish(OrderEmailEvent event, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_EMAIL_EXCHANGE, routingKey, event);
            log.info("Published order email event. exchange={}, routingKey={}, orderId={}, eventType={}",
                    RabbitMqConfig.ORDER_EMAIL_EXCHANGE,
                    routingKey,
                    event != null ? event.getOrderId() : null,
                    event != null ? event.getEventType() : null);
        } catch (Exception exception) {
            log.error("Failed to publish order email event. routingKey={}, orderId={}, eventType={}",
                    routingKey,
                    event != null ? event.getOrderId() : null,
                    event != null ? event.getEventType() : null,
                    exception);
        }
    }
}
