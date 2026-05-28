package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.repository.OrderEntityRepository;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderEntityServiceImpl extends AbstractCrudService<OrderEntity, Integer> implements OrderEntityService {

    private final OrderEntityRepository repository;

    @Override
    protected JpaRepository<OrderEntity, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<OrderEntity> findByAccountId(Integer accountId) {
        return repository.findByAccountAccountIdOrderByOrderDateDescOrderIdDesc(accountId);
    }

    @Override
    public List<OrderEntity> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<OrderEntity> findAllOrderByNewest() {
        return repository.findAllByOrderByOrderDateDescOrderIdDesc();
    }

    @Override
    public Optional<OrderEntity> findByOrderId(Integer orderId) {
        return repository.findByOrderId(orderId);
    }

    @Override
    public Page<OrderEntity> findPageByAccountId(Integer accountId, Pageable pageable) {
        return repository.findByAccountAccountIdOrderByOrderDateDesc(accountId, pageable);
    }
}
