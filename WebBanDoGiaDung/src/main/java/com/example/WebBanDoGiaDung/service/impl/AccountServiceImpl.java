package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.repository.AccountRepository;
import com.example.WebBanDoGiaDung.service.AccountService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl extends AbstractCrudService<Account, Integer> implements AccountService {

    private final AccountRepository repository;

    @Override
    protected JpaRepository<Account, Integer> getRepository() {
        return repository;
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public List<Account> findByRole(Integer role) {
        return repository.findByRole(role);
    }

    @Override
    public List<Account> findByStatus(String status) {
        return repository.findByStatus(status);
    }
}
