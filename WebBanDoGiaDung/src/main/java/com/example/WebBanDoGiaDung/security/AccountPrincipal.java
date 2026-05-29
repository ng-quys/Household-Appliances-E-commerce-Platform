package com.example.WebBanDoGiaDung.security;

import com.example.WebBanDoGiaDung.security.dto.AccountSessionDto;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class AccountPrincipal implements UserDetails, OAuth2User, Serializable {

    private static final long serialVersionUID = 1L;

    private final AccountSessionDto account;
    private final Map<String, Object> attributes;
    private final String password;

    public AccountPrincipal(AccountSessionDto account, String password) {
        this(account, password, Map.of());
    }

    public AccountPrincipal(AccountSessionDto account, String password, Map<String, Object> attributes) {
        this.account = account;
        this.password = password;
        this.attributes = attributes != null ? attributes : Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Integer role = account != null ? account.getRole() : null;

        String roleName = switch (role == null ? 1 : role) {
            case 0 -> "ADMIN";
            case 2 -> "STAFF";
            default -> "USER";
        };

        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return account != null ? account.getEmail() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return account != null && "1".equals(account.getStatus());
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return account != null ? account.getEmail() : null;
    }

    public AccountSessionDto getAccount() {
        return account;
    }

    public Integer getAccountId() {
        return account != null ? account.getAccountId() : null;
    }

    public String getEmail() {
        return account != null ? account.getEmail() : null;
    }

    public Integer getRole() {
        return account != null ? account.getRole() : null;
    }
}
