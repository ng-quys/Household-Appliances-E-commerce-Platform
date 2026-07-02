package com.example.WebBanDoGiaDung.security;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentAccountService {

    private final AccountService accountService;

    public Optional<Account> getCurrentAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AccountPrincipal accountPrincipal) {
            Integer accountId = accountPrincipal.getAccountId();

            if (accountId != null) {
                return accountService.findById(accountId);
            }

            String email = accountPrincipal.getUsername();
            if (email != null && !email.isBlank()) {
                return accountService.findByEmail(email.trim());
            }
        }

        String email = authentication.getName();
        if (email != null && !email.isBlank()) {
            return accountService.findByEmail(email.trim());
        }

        return Optional.empty();
    }
}