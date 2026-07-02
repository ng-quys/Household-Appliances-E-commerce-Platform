package com.example.WebBanDoGiaDung.service;

import java.util.Optional;

public interface PasswordResetService {
    void requestPasswordReset(String email);

    boolean isValidToken(String token);

    Optional<String> findEmailByToken(String token);

    void resetPassword(String token, String newPassword, String confirmPassword);
}
