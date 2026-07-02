package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.Discount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Integer> {
    Optional<Discount> findByDiscountCode(String discountCode);
}
