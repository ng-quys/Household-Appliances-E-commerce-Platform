package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.OrderAddress;
import java.util.List;

public interface OrderAddressService extends CrudService<OrderAddress, Integer> {
    List<OrderAddress> searchByOrderUsername(String username);
}
