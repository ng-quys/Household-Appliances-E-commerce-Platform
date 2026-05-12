package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Account;
import java.util.List;
import java.util.Optional;

public interface AccountService extends CrudService<Account, Integer> {
    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Account> findByRole(Integer role);

    List<Account> findByStatus(String status);
}
