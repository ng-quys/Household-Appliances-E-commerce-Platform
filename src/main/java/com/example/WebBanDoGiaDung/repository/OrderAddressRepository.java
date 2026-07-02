package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.OrderAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderAddressRepository extends JpaRepository<OrderAddress, Integer> {
    List<OrderAddress> findByOrderUsernameContainingIgnoreCase(String username);
}
