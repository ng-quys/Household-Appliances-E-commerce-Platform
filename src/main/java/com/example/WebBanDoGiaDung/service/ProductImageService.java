package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.ProductImage;
import java.util.List;

public interface ProductImageService extends CrudService<ProductImage, Integer> {
    List<ProductImage> findByProductId(Integer productId);
}
