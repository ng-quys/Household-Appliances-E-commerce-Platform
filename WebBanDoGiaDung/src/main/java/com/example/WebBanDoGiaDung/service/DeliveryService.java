package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Delivery;
import java.util.List;

public interface DeliveryService extends CrudService<Delivery, Integer> {
    List<Delivery> findByStatus(String status);
}
