package com.example.WebBanDoGiaDung.specification.admin;

import com.example.WebBanDoGiaDung.entity.OrderEntity;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class AdminOrderSpecification {

    private AdminOrderSpecification() {
    }

    public static Specification<OrderEntity> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.trim().isBlank()) {
                return null;
            }
            return cb.equal(root.get("status"), status.trim());
        };
    }

    public static Specification<OrderEntity> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isBlank()) {
                return null;
            }
            String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("orderId").as(String.class)), keyword),
                    cb.like(cb.lower(root.join("account", jakarta.persistence.criteria.JoinType.LEFT).get("name")), keyword),
                    cb.like(cb.lower(root.join("account", jakarta.persistence.criteria.JoinType.LEFT).get("email")), keyword),
                    cb.like(cb.lower(root.join("account", jakarta.persistence.criteria.JoinType.LEFT).get("phone")), keyword),
                    cb.like(cb.lower(root.join("orderAddress", jakarta.persistence.criteria.JoinType.LEFT).get("orderUsername")), keyword),
                    cb.like(cb.lower(root.join("orderAddress", jakarta.persistence.criteria.JoinType.LEFT).get("orderPhoneNumber")), keyword)
            );
        };
    }

    public static Specification<OrderEntity> filter(String search, String status) {
        return Specification.where(search(search))
                .and(hasStatus(status));
    }
}
