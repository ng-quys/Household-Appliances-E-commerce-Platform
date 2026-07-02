package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Brand;
import com.example.WebBanDoGiaDung.repository.BrandRepository;
import com.example.WebBanDoGiaDung.service.BrandService;
import com.example.WebBanDoGiaDung.specification.admin.AdminBrandSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<Brand> findAdminBrands(String search, Pageable pageable) {
        return repository.findAll(AdminBrandSpecification.filter(search), pageable);
    }
}
