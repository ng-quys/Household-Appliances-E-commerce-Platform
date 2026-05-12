package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Province;
import java.util.List;

public interface ProvinceService extends CrudService<Province, Integer> {
    List<Province> findAllOrderByName();
}
