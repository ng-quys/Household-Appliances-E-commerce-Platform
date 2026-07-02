package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.District;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistrictRepository extends JpaRepository<District, Integer> {
    List<District> findByProvinceProvinceId(Integer provinceId);
}
