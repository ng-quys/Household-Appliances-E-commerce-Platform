package com.example.WebBanDoGiaDung.specification.admin;

import com.example.WebBanDoGiaDung.entity.Brand;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class AdminBrandSpecification {

    private AdminBrandSpecification() {
    }

    public static Specification<Brand> searchByName(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isBlank()) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("brandName")),
                    "%" + search.trim().toLowerCase(Locale.ROOT) + "%"
            );
        };
    }

    public static Specification<Brand> filter(String search) {
        return Specification.where(searchByName(search));
    }
}
