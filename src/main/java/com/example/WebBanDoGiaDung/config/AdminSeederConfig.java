package com.example.WebBanDoGiaDung.config;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class AdminSeederConfig {

    @Bean
    public CommandLineRunner seedAdmin(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@gmail.com";
            java.util.Optional<Account> existingAdmin = accountRepository.findByEmail(adminEmail);
            if (existingAdmin.isEmpty()) {
                Account admin = new Account();
                admin.setName("Admin Hệ Thống");
                admin.setEmail(adminEmail);
                admin.setPhone("0987654321");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(0); // Role 0 = ADMIN trong hệ thống
                admin.setStatus("1"); // 1 = Hoạt động
                admin.setCreateAt(LocalDateTime.now());
                accountRepository.save(admin);
                System.out.println("============================================");
                System.out.println("   ĐÃ KHỞI TẠO TÀI KHOẢN ADMIN MẶC ĐỊNH");
                System.out.println("   Email: " + adminEmail);
                System.out.println("   Mật khẩu: admin123");
                System.out.println("============================================");
            } else {
                Account admin = existingAdmin.get();
                if (admin.getPassword() == null || !admin.getPassword().startsWith("$2a$")) {
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    accountRepository.save(admin);
                    System.out.println("============================================");
                    System.out.println("   ĐÃ MÃ HÓA LẠI MẬT KHẨU ADMIN VỀ: admin123");
                    System.out.println("============================================");
                } else {
                    System.out.println("-> Tài khoản admin@gmail.com đã tồn tại và mật khẩu đã mã hóa BCrypt.");
                }
            }
        };
    }
}
