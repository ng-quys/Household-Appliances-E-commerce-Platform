package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Ward;
import java.util.List;

public interface WardService extends CrudService<Ward, Integer> {
    List<Ward> findByDistrictId(Integer districtId);
}
