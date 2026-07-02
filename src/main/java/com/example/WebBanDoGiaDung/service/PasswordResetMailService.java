package com.example.WebBanDoGiaDung.service;

public interface PasswordResetMailService {
    void sendResetLink(String recipientEmail, String resetLink);
}
