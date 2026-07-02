package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.Province;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {
    List<Province> findAllByOrderByProvinceNameAsc();
}
