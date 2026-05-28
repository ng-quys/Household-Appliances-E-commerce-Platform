package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.OrderEntity;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderEntityRepository extends JpaRepository<OrderEntity, Integer> {
    List<OrderEntity> findByAccountAccountId(Integer accountId);

    List<OrderEntity> findByAccountAccountIdOrderByOrderDateDescOrderIdDesc(Integer accountId);

    List<OrderEntity> findByStatus(String status);

    List<OrderEntity> findTop10ByOrderByOrderDateDescOrderIdDesc();

    List<OrderEntity> findAllByOrderByOrderDateDescOrderIdDesc();

    @EntityGraph(attributePaths = {"account", "payment", "delivery", "orderAddress", "orderAddress.province", "orderAddress.district", "orderAddress.ward"})
    Optional<OrderEntity> findByOrderId(Integer orderId);

    Page<OrderEntity> findByAccountAccountIdOrderByOrderDateDesc(Integer accountId, Pageable pageable);

}
