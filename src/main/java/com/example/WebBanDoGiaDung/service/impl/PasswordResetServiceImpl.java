package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.PasswordResetMailService;
import com.example.WebBanDoGiaDung.service.PasswordResetService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final String TOKEN_PREFIX = "password-reset:";

    private final AccountService accountService;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailService passwordResetMailService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Override
    public void requestPasswordReset(String email) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }

        accountService.findByEmail(normalizedEmail).ifPresent(account -> {
            String token = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
            String key = TOKEN_PREFIX + token;
            stringRedisTemplate.opsForValue().set(key, String.valueOf(account.getAccountId()), TOKEN_TTL);
            passwordResetMailService.sendResetLink(account.getEmail(), buildResetLink(token));
        });
    }

    @Override
    public boolean isValidToken(String token) {
        return resolveAccountId(token).isPresent();
    }

    @Override
    public Optional<String> findEmailByToken(String token) {
        return resolveAccountId(token)
                .flatMap(accountService::findById)
                .map(Account::getEmail);
    }

    @Override
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu mới không được để trống.");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu nhập lại không khớp.");
        }

        Integer accountId = resolveAccountId(token)
                .orElseThrow(() -> new IllegalArgumentException("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));

        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setUpdateAt(LocalDateTime.now());
        accountService.save(account);
        stringRedisTemplate.delete(TOKEN_PREFIX + token);
    }

    private Optional<Integer> resolveAccountId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String rawValue = stringRedisTemplate.opsForValue().get(TOKEN_PREFIX + token.trim());
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(rawValue));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String buildResetLink(String token) {
        String baseUrl = appBaseUrl == null ? "http://localhost" : appBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/reset-password?token=" + token;
    }
}
