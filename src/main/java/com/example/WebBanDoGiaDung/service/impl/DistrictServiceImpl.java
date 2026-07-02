package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.District;
import com.example.WebBanDoGiaDung.repository.DistrictRepository;
import com.example.WebBanDoGiaDung.service.DistrictService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistrictServiceImpl extends AbstractCrudService<District, Integer> implements DistrictService {

    private final DistrictRepository repository;

    @Override
    protected JpaRepository<District, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<District> findByProvinceId(Integer provinceId) {
        return repository.findByProvinceProvinceId(provinceId);
    }
}
