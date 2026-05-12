package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.OrderAddress;
import com.example.WebBanDoGiaDung.repository.OrderAddressRepository;
import com.example.WebBanDoGiaDung.service.OrderAddressService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderAddressServiceImpl extends AbstractCrudService<OrderAddress, Integer> implements OrderAddressService {

    private final OrderAddressRepository repository;

    @Override
    protected JpaRepository<OrderAddress, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<OrderAddress> searchByOrderUsername(String username) {
        return repository.findByOrderUsernameContainingIgnoreCase(username);
    }
}
