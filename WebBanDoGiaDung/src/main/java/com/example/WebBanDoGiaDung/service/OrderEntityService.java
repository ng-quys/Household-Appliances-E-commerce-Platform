package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.OrderEntity;
import java.util.List;
import java.util.Optional;

public interface OrderEntityService extends CrudService<OrderEntity, Integer> {
    List<OrderEntity> findByAccountId(Integer accountId);

    List<OrderEntity> findByStatus(String status);

    List<OrderEntity> findAllOrderByNewest();

    Optional<OrderEntity> findByOrderId(Integer orderId);
}
