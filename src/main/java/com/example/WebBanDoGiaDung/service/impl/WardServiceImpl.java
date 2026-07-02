package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Ward;
import com.example.WebBanDoGiaDung.repository.WardRepository;
import com.example.WebBanDoGiaDung.service.WardService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WardServiceImpl extends AbstractCrudService<Ward, Integer> implements WardService {

    private final WardRepository repository;

    @Override
    protected JpaRepository<Ward, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<Ward> findByDistrictId(Integer districtId) {
        return repository.findByDistrictDistrictId(districtId);
    }
}
