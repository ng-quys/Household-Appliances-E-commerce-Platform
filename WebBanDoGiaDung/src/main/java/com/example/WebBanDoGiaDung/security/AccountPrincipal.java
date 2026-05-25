package com.example.WebBanDoGiaDung.security;

import com.example.WebBanDoGiaDung.entity.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AccountPrincipal implements UserDetails, OAuth2User {

    private final Account account;
    private Map<String, Object> attributes;

    // Constructor cho login thường (không OAuth2)
    public AccountPrincipal(Account account) {
        this.account = account;
    }

    // Constructor cho OAuth2 (có attributes)
    public AccountPrincipal(Account account, Map<String, Object> attributes) {
        this.account = account;
        this.attributes = attributes != null ? attributes : Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Integer role = account.getRole();

        String roleName = switch (role == null ? 1 : role) {
            case 0 -> "ADMIN";
            case 2 -> "STAFF";
            default -> "USER";
        };

        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword() {
        return account.getPassword();
    }

    @Override
    public String getUsername() {
        return account.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return "1".equals(account.getStatus()); }

    // === OAuth2User methods ===
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return account.getEmail();
    }

    public Account getAccount() {
        return account;
    }
}