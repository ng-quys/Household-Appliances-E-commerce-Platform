package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByStatus(String status);

    List<Product> findByGenreGenreId(Integer genreId);

    List<Product> findByBrandBrandId(Integer brandId);

    long countByBrandBrandId(Integer brandId);

    List<Product> findByProductNameContainingIgnoreCase(String keyword);

    List<Product> findTop5ByOrderByProductIdDesc();
}
