package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Payment;
import com.example.WebBanDoGiaDung.repository.PaymentRepository;
import com.example.WebBanDoGiaDung.service.PaymentService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends AbstractCrudService<Payment, Integer> implements PaymentService {

    private final PaymentRepository repository;

    @Override
    protected JpaRepository<Payment, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<Payment> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    @Override
    public Optional<Payment> findActiveByPaymentName(String paymentName) {
        return repository.findByPaymentNameIgnoreCaseAndStatus(paymentName, "1");
    }

    @Override
    public Optional<Payment> findActiveByPaymentNames(List<String> paymentNames) {
        return repository.findByStatusAndPaymentNameIgnoreCaseIn("1", paymentNames).stream().findFirst();
    }
}
