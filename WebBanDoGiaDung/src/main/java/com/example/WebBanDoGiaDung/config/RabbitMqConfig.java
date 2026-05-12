package com.example.WebBanDoGiaDung.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EMAIL_EXCHANGE = "order.email.exchange";
    public static final String ORDER_EMAIL_QUEUE = "order.email.queue";
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";
    public static final String ROUTING_KEY_ORDER_PAID = "order.paid";

    @Bean
    public DirectExchange orderEmailExchange() {
        return new DirectExchange(ORDER_EMAIL_EXCHANGE);
    }

    @Bean
    public Queue orderEmailQueue() {
        return new Queue(ORDER_EMAIL_QUEUE, true);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderEmailQueue, DirectExchange orderEmailExchange) {
        return BindingBuilder.bind(orderEmailQueue)
                .to(orderEmailExchange)
                .with(ROUTING_KEY_ORDER_CREATED);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderEmailQueue, DirectExchange orderEmailExchange) {
        return BindingBuilder.bind(orderEmailQueue)
                .to(orderEmailExchange)
                .with(ROUTING_KEY_ORDER_PAID);
    }

    @Bean
    public ObjectMapper rabbitObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper rabbitObjectMapper) {
        return new Jackson2JsonMessageConverter(rabbitObjectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }
}
