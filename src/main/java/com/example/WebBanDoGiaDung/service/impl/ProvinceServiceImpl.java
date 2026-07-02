package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Province;
import com.example.WebBanDoGiaDung.repository.ProvinceRepository;
import com.example.WebBanDoGiaDung.service.ProvinceService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProvinceServiceImpl extends AbstractCrudService<Province, Integer> implements ProvinceService {

    private final ProvinceRepository repository;

    @Override
    protected JpaRepository<Province, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<Province> findAllOrderByName() {
        return repository.findAllByOrderByProvinceNameAsc();
    }
}
