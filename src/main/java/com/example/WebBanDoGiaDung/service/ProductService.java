package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductService extends CrudService<Product, Integer> {
    List<Product> findByStatus(String status);

    List<Product> findByGenreId(Integer genreId);

    List<Product> findByBrandId(Integer brandId);

    List<Product> searchByName(String keyword);

    List<ProductCacheDto> findActiveProductCards();

    Optional<ProductCacheDto> findProductDetailById(Integer id);

    List<ProductCacheDto> findActiveProductsByGenreId(Integer genreId);

    List<ProductCacheDto> findHeaderPreviewProductsByGenreId(Integer genreId, int limit);

    List<ProductCacheDto> findFeaturedProducts(int limit);

    List<ProductCacheDto> findRelatedProducts(Integer currentProductId, Integer genreId, int limit);

    List<ProductCacheDto> applyPriceSort(List<ProductCacheDto> products, String sort);

    Page<ProductCacheDto> findActiveProducts(Pageable pageable);

    Page<Product> findAdminProducts(String search, String statusFilter, Pageable pageable);

    Page<ProductCacheDto> searchProducts(String keyword, Integer genreId, Integer brandId,
                                         Double minPrice, Double maxPrice, Pageable pageable);
}
