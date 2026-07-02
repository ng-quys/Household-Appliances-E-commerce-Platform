package com.example.WebBanDoGiaDung.security;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.repository.AccountRepository;
import com.example.WebBanDoGiaDung.security.dto.AccountSessionDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(AccountRepository accountRepository,
                                   PasswordEncoder passwordEncoder) {

        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes() != null
                ? oauth2User.getAttributes()
                : Map.of());

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String providerId = (String) attributes.get("sub");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email from Google is required");
        }

        // Tìm hoặc tạo tài khoản
        Account account = accountRepository.findByEmail(email)
                .orElseGet(() -> {
                    Account newAccount = new Account();
                    newAccount.setEmail(email);
                    newAccount.setName(name);
                    newAccount.setAvatar(picture);
                    newAccount.setProvider("google");
                    newAccount.setProviderId(providerId);
                    newAccount.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));                    newAccount.setRole(1); // USER
                    newAccount.setPhone("");
                    newAccount.setStatus("1");
                    newAccount.setCreateAt(LocalDateTime.now());
                    newAccount.setCreateBy("google-oauth");
                    return accountRepository.save(newAccount);
                });

        // Cập nhật avatar nếu thay đổi
        if (picture != null && !picture.equals(account.getAvatar())) {
            account.setAvatar(picture);
            accountRepository.save(account);
        }

        System.out.println("✅ OAuth2 User Loaded: " + email + " | Attributes: " + attributes.keySet());

        // Trả về CustomUserDetails có cả UserDetails + OAuth2User
        return new AccountPrincipal(AccountSessionDto.fromAccount(account), account.getPassword(), attributes);
    }
}