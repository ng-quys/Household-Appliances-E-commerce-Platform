package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByStatus(String status);

    Optional<Payment> findByPaymentNameIgnoreCaseAndStatus(String paymentName, String status);

    List<Payment> findByStatusAndPaymentNameIgnoreCaseIn(String status, List<String> paymentNames);
}
