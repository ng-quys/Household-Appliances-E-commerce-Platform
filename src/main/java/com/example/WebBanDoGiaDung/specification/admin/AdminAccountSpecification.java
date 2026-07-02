package com.example.WebBanDoGiaDung.specification.admin;

import com.example.WebBanDoGiaDung.entity.Account;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class AdminAccountSpecification {

    private AdminAccountSpecification() {
    }

    public static Specification<Account> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isBlank()) {
                return null;
            }
            String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), keyword),
                    cb.like(cb.lower(root.get("email")), keyword),
                    cb.like(cb.lower(root.get("phone")), keyword)
            );
        };
    }

    public static Specification<Account> filter(String search) {
        return Specification.where(search(search));
    }
}
