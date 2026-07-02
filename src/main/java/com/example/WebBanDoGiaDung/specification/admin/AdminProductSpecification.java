package com.example.WebBanDoGiaDung.specification.admin;

import com.example.WebBanDoGiaDung.entity.Product;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class AdminProductSpecification {

    private AdminProductSpecification() {
    }

    public static Specification<Product> nameContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isBlank()) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("productName")),
                    "%" + search.trim().toLowerCase(Locale.ROOT) + "%"
            );
        };
    }

    public static Specification<Product> hasStatusFilter(String statusFilter) {
        return (root, query, cb) -> {
            if (statusFilter == null || statusFilter.isBlank() || "all".equalsIgnoreCase(statusFilter)) {
                return null;
            }
            if ("visible".equalsIgnoreCase(statusFilter)) {
                return cb.equal(root.get("status"), "1");
            }
            if ("hidden".equalsIgnoreCase(statusFilter)) {
                return cb.notEqual(root.get("status"), "1");
            }
            return null;
        };
    }

    public static Specification<Product> filter(String search, String statusFilter) {
        return Specification.where(nameContains(search))
                .and(hasStatusFilter(statusFilter));
    }
}
