package com.example.WebBanDoGiaDung.specification;

import com.example.WebBanDoGiaDung.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> hasStatus(String status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> hasGenreId(Integer genreId) {
        return (root, query, cb) ->
                genreId == null ? null : cb.equal(root.get("genre").get("genreId"), genreId);
    }

    public static Specification<Product> hasBrandId(Integer brandId) {
        return (root, query, cb) ->
                brandId == null ? null : cb.equal(root.get("brand").get("brandId"), brandId);
    }

    public static Specification<Product> priceBetween(Double min, Double max) {
        return (root, query, cb) -> {
            if (min == null && max == null) {
                return null;
            }

            if (min != null && max != null) {
                return cb.between(root.get("price"), min, max);
            }

            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), min);
            }

            return cb.lessThanOrEqualTo(root.get("price"), max);
        };
    }

    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) ->
                keyword == null || keyword.isBlank()
                        ? null
                        : cb.like(
                        cb.lower(root.get("productName")),
                        "%" + keyword.trim().toLowerCase() + "%"
                );
    }

    public static Specification<Product> search(
            String keyword,
            Integer genreId,
            Integer brandId,
            Double minPrice,
            Double maxPrice
    ) {
        return Specification.where(hasStatus("1"))
                .and(nameContains(keyword))
                .and(hasGenreId(genreId))
                .and(hasBrandId(brandId))
                .and(priceBetween(minPrice, maxPrice));
    }
}