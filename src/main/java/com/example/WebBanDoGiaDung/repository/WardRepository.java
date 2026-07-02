package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.Ward;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WardRepository extends JpaRepository<Ward, Integer> {
    List<Ward> findByDistrictDistrictId(Integer districtId);
}
