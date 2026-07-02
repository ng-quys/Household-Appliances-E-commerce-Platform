package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.service.PasswordResetMailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetMailServiceImpl implements PasswordResetMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Override
    public void sendResetLink(String recipientEmail, String resetLink) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("Password reset mail skipped because mail config is missing. recipient={}", recipientEmail);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("Đặt lại mật khẩu tài khoản");
            helper.setText(buildHtml(resetLink), true);
            mailSender.send(mimeMessage);
        } catch (Exception exception) {
            log.error("Failed to send password reset email. recipient={}", recipientEmail, exception);
        }
    }

    private String buildHtml(String resetLink) {
        return """
                <div style=\"font-family:Arial,sans-serif;line-height:1.6;color:#172033\">
                    <h2>Đặt lại mật khẩu</h2>
                    <p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản Web Bán Đồ Gia Dụng.</p>
                    <p>Liên kết này có hiệu lực trong <strong>15 phút</strong> và chỉ dùng được một lần.</p>
                    <p><a href=\"%s\" style=\"display:inline-block;padding:10px 16px;background:#facc15;color:#172033;text-decoration:none;border-radius:8px;font-weight:700\">Đặt lại mật khẩu</a></p>
                    <p>Nếu bạn không yêu cầu thao tác này, có thể bỏ qua email.</p>
                </div>
                """.formatted(resetLink);
    }
}
