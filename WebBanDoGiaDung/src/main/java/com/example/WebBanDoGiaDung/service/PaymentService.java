package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentService extends CrudService<Payment, Integer> {
    List<Payment> findByStatus(String status);

    Optional<Payment> findActiveByPaymentName(String paymentName);

    Optional<Payment> findActiveByPaymentNames(List<String> paymentNames);
}
