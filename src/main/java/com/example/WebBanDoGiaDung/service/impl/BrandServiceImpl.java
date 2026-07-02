package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Brand;
import com.example.WebBanDoGiaDung.repository.BrandRepository;
import com.example.WebBanDoGiaDung.service.BrandService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl extends AbstractCrudService<Brand, Integer> implements BrandService {

    private final BrandRepository repository;

    @Override
    protected JpaRepository<Brand, Integer> getRepository() {
        return repository;
    }

    @Override
    public Optional<Brand> findByBrandName(String brandName) {
        return repository.findByBrandName(brandName);
    }
}
