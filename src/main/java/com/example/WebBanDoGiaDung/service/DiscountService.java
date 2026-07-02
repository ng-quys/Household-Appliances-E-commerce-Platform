package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Discount;
import java.util.Optional;

public interface DiscountService extends CrudService<Discount, Integer> {
    Optional<Discount> findByDiscountCode(String discountCode);
}
