package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.District;
import java.util.List;

public interface DistrictService extends CrudService<District, Integer> {
    List<District> findByProvinceId(Integer provinceId);
}
