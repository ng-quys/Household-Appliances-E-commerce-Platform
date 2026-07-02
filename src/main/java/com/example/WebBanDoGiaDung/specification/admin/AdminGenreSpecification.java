package com.example.WebBanDoGiaDung.specification.admin;

import com.example.WebBanDoGiaDung.entity.Genre;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class AdminGenreSpecification {

    private AdminGenreSpecification() {
    }

    public static Specification<Genre> searchByName(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isBlank()) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("genreName")),
                    "%" + search.trim().toLowerCase(Locale.ROOT) + "%"
            );
        };
    }

    public static Specification<Genre> filter(String search) {
        return Specification.where(searchByName(search));
    }
}
