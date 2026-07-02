package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Brand;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandService extends CrudService<Brand, Integer> {
    Optional<Brand> findByBrandName(String brandName);

    Page<Brand> findAdminBrands(String search, Pageable pageable);
}
