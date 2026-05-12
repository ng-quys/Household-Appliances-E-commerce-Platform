package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Discount;
import com.example.WebBanDoGiaDung.repository.DiscountRepository;
import com.example.WebBanDoGiaDung.service.DiscountService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl extends AbstractCrudService<Discount, Integer> implements DiscountService {

    private final DiscountRepository repository;

    @Override
    protected JpaRepository<Discount, Integer> getRepository() {
        return repository;
    }

    @Override
    public Optional<Discount> findByDiscountCode(String discountCode) {
        return repository.findByDiscountCode(discountCode);
    }
}
