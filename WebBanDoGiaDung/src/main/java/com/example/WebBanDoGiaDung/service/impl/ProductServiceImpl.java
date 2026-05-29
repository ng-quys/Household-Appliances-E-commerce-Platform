package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.repository.ProductRepository;
import com.example.WebBanDoGiaDung.service.ProductService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.example.WebBanDoGiaDung.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends AbstractCrudService<Product, Integer> implements ProductService {

    private final ProductRepository repository;

    @Override
    protected JpaRepository<Product, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<Product> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<Product> findByGenreId(Integer genreId) {
        return repository.findByGenreGenreId(genreId);
    }

    @Override
    public List<Product> findByBrandId(Integer brandId) {
        return repository.findByBrandBrandId(brandId);
    }

    @Override
    public List<Product> searchByName(String keyword) {
        return repository.findByProductNameContainingIgnoreCase(keyword);
    }

    @Override
    @Cacheable(cacheNames = "productList", key = "'active'")
    public List<ProductCacheDto> findActiveProductCards() {
        return repository.findByStatus("1").stream()
                .map(ProductCacheDto::fromEntity)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "productDetail", key = "#id")
    public Optional<ProductCacheDto> findProductDetailById(Integer id) {
        return repository.findById(id)
                .map(ProductCacheDto::fromEntity);
    }

    @Override
    @Cacheable(cacheNames = "productByGenre", key = "#genreId")
    public List<ProductCacheDto> findActiveProductsByGenreId(Integer genreId) {
        return repository.findByGenreGenreId(genreId).stream()
                .filter(product -> "1".equals(product.getStatus()))
                .map(ProductCacheDto::fromEntity)
                .toList();
    }

    @Override
    public List<ProductCacheDto> findHeaderPreviewProductsByGenreId(Integer genreId, int limit) {
        return findActiveProductsByGenreId(genreId).stream()
                .sorted((left, right) -> {
                    long rightScore = right.getBuyturn() == null ? 0L : right.getBuyturn();
                    long leftScore = left.getBuyturn() == null ? 0L : left.getBuyturn();
                    return Long.compare(rightScore, leftScore);
                })
                .limit(Math.max(limit, 0))
                .toList();
    }

    @Override
    public List<ProductCacheDto> findFeaturedProducts(int limit) {
        return findActiveProductCards().stream()
                .sorted((left, right) -> {
                    long rightScore = right.getBuyturn() == null ? 0L : right.getBuyturn();
                    long leftScore = left.getBuyturn() == null ? 0L : left.getBuyturn();
                    return Long.compare(rightScore, leftScore);
                })
                .limit(limit)
                .toList();
    }

    @Override
    public List<ProductCacheDto> applyPriceSort(List<ProductCacheDto> products, String sort) {
        if (products == null) {
            return List.of();
        }
        if ("priceAsc".equalsIgnoreCase(sort)) {
            return products.stream()
                    .sorted(Comparator.comparing(product -> product.getPrice() == null ? 0D : product.getPrice()))
                    .toList();
        }
        if ("priceDesc".equalsIgnoreCase(sort)) {
            return products.stream()
                    .sorted(Comparator.comparing((ProductCacheDto product) -> product.getPrice() == null ? 0D : product.getPrice()).reversed())
                    .toList();
        }
        return products;
    }

    @Override
    @CacheEvict(cacheNames = {"productList", "productDetail", "productByGenre"}, allEntries = true)
    public Product save(Product entity) {
        return repository.save(entity);
    }

    @Override
    @CacheEvict(cacheNames = {"productList", "productDetail", "productByGenre"}, allEntries = true)
    public Product update(Integer id, Product entity) {
        return super.update(id, entity);
    }

    @Override
    @CacheEvict(cacheNames = {"productList", "productDetail", "productByGenre"}, allEntries = true)
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public Page<ProductCacheDto> findActiveProducts(Pageable pageable) {
        Page<Product> page = repository.findAll(ProductSpecification.hasStatus("1"), pageable);

        return page.map(ProductCacheDto::fromEntity);
    }

    @Override
    public Page<ProductCacheDto> searchProducts(String keyword, Integer genreId, Integer brandId,
                                                Double minPrice, Double maxPrice, Pageable pageable) {

        Page<Product> page = repository.findAll(ProductSpecification.search(keyword, genreId, brandId, minPrice, maxPrice), pageable);

        return page.map(ProductCacheDto::fromEntity);
    }
}
