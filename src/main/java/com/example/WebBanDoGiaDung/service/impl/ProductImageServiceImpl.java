package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.ProductImage;
import com.example.WebBanDoGiaDung.repository.ProductImageRepository;
import com.example.WebBanDoGiaDung.service.ProductImageService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl extends AbstractCrudService<ProductImage, Integer> implements ProductImageService {

    private final ProductImageRepository repository;

    @Override
    protected JpaRepository<ProductImage, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<ProductImage> findByProductId(Integer productId) {
        return repository.findByProductProductId(productId);
    }
}
