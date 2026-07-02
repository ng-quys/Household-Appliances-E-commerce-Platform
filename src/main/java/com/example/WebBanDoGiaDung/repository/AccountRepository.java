package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Account> findByRole(Integer role);

    List<Account> findByStatus(String status);

    List<Account> findTop5ByOrderByCreateAtDescAccountIdDesc();
}
