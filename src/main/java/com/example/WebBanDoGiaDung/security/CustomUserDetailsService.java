package com.example.WebBanDoGiaDung.security;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.repository.AccountRepository;
import com.example.WebBanDoGiaDung.security.dto.AccountSessionDto;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("customUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public CustomUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found with email: " + email));

        return new AccountPrincipal(AccountSessionDto.fromAccount(account), account.getPassword());
    }
}
