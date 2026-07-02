package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Brand;
import java.util.Optional;

public interface BrandService extends CrudService<Brand, Integer> {
    Optional<Brand> findByBrandName(String brandName);
}
