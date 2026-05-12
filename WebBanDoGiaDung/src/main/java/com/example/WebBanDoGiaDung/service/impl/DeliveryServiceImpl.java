package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Delivery;
import com.example.WebBanDoGiaDung.repository.DeliveryRepository;
import com.example.WebBanDoGiaDung.service.DeliveryService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl extends AbstractCrudService<Delivery, Integer> implements DeliveryService {

    private final DeliveryRepository repository;

    @Override
    protected JpaRepository<Delivery, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<Delivery> findByStatus(String status) {
        return repository.findByStatus(status);
    }
}
