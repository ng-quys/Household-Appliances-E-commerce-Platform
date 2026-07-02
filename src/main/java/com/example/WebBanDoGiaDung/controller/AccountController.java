package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.AccountAddress;
import com.example.WebBanDoGiaDung.service.AccountAddressService;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import com.example.WebBanDoGiaDung.service.PasswordResetService;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
public class AccountController {

    private final AccountService accountService;
    private final AccountAddressService accountAddressService;
    private final OrderEntityService orderEntityService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;

    public AccountController(AccountService accountService,
                             AccountAddressService accountAddressService,
                             OrderEntityService orderEntityService,
                             PasswordEncoder passwordEncoder,
                             PasswordResetService passwordResetService) {
        this.accountService = accountService;
        this.accountAddressService = accountAddressService;
        this.orderEntityService = orderEntityService;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("account", new Account());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerPost(@RequestParam String name,
                               @RequestParam @Email String email,
                               @RequestParam String phone,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email không được để trống.");
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phone", phone);
            return "redirect:/register";
        }
        if (accountService.findByEmail(normalizedEmail).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại!");
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phone", phone);
            return "redirect:/register";
        }
        if (password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu không được để trống.");
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phone", phone);
            return "redirect:/register";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu nhập lại không khớp.");
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phone", phone);
            return "redirect:/register";
        }
        if (phone == null || phone.isBlank() || phone.length() > 10 || !phone.matches("[0-9]{9,10}")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ, tối đa 10 ký tự số.");
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phone", phone);
            return "redirect:/register";
        }

        Account account = new Account();
        account.setName(name == null || name.isBlank() ? normalizedEmail : name.trim());
        account.setEmail(normalizedEmail);
        account.setPhone(phone.trim());
        account.setPassword(passwordEncoder.encode(password));
        account.setRole(1);
        account.setStatus("1");
        account.setCreateAt(LocalDateTime.now());
        accountService.save(account);
        return "redirect:/login?registered";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordPost(@RequestParam String email,
                                     RedirectAttributes redirectAttributes) {
        passwordResetService.requestPasswordReset(email);
        redirectAttributes.addFlashAttribute("success",
                "Nếu email tồn tại, hệ thống đã gửi hướng dẫn đặt lại mật khẩu.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordToken(@RequestParam(required = false) String token,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (!passwordResetService.isValidToken(token)) {
            redirectAttributes.addFlashAttribute("error", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "redirect:/forgot-password";
        }
        model.addAttribute("token", token);
        model.addAttribute("email", passwordResetService.findEmailByToken(token).orElse(""));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordTokenPost(@RequestParam String token,
                                         @RequestParam String password,
                                         @RequestParam String confirmPassword,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        try {
            passwordResetService.resetPassword(token, password, confirmPassword);
            return "redirect:/login?resetSuccess";
        } catch (IllegalArgumentException exception) {
            if (!passwordResetService.isValidToken(token)) {
                redirectAttributes.addFlashAttribute("error", exception.getMessage());
                return "redirect:/forgot-password";
            }
            model.addAttribute("token", token);
            model.addAttribute("email", passwordResetService.findEmailByToken(token).orElse(""));
            model.addAttribute("error", exception.getMessage());
            return "auth/reset-password";
        }
    }

    @GetMapping("/account/change-password")
    public String changePassword() {
        return "account/change-password";
    }

    @PostMapping("/account/change-password")
    public String changePasswordPost(@RequestParam Integer accountId,
                                     @RequestParam String newPassword,
                                     RedirectAttributes redirectAttributes) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setUpdateAt(LocalDateTime.now());
        accountService.save(account);
        redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công.");
        return "redirect:/account/edit-profile?accountId=" + accountId;
    }

    @GetMapping("/account/reset-password")
    public String resetPassword() {
        return "account/reset-password";
    }

    @PostMapping("/account/reset-password")
    public String resetPasswordPost(@RequestParam String email,
                                    @RequestParam String newPassword,
                                    RedirectAttributes redirectAttributes) {
        Optional<Account> account = accountService.findByEmail(email == null ? null : email.trim().toLowerCase());
        if (account.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email không tồn tại!");
            return "redirect:/account/reset-password";
        }
        account.get().setPassword(passwordEncoder.encode(newPassword));
        account.get().setUpdateAt(LocalDateTime.now());
        accountService.save(account.get());
        redirectAttributes.addFlashAttribute("success", "Reset mật khẩu thành công!");
        return "redirect:/login";
    }

    @GetMapping("/account/edit-profile")
    public String editProfile(@RequestParam(required = false) Integer accountId, Model model) {
        if (accountId != null) {
            accountService.findById(accountId).ifPresent(acc -> model.addAttribute("account", acc));
        }
        return "account/edit-profile";
    }

    @PostMapping("/account/edit-profile")
    public String editProfilePost(Account account, RedirectAttributes redirectAttributes) {
        account.setUpdateAt(LocalDateTime.now());
        accountService.save(account);
        redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công.");
        return "redirect:/account/edit-profile?accountId=" + account.getAccountId();
    }

    @GetMapping("/account/address")
    public String address(@RequestParam Integer accountId, Model model) {
        List<AccountAddress> addresses = accountAddressService.findByAccountId(accountId);
        model.addAttribute("addresses", addresses);
        model.addAttribute("accountId", accountId);
        return "account/address";
    }

    @GetMapping("/account/address/create")
    public String createAddress(Model model, @RequestParam Integer accountId) {
        AccountAddress address = new AccountAddress();
        address.setAccountId(accountId);
        model.addAttribute("address", address);
        return "account/create-address";
    }

    @PostMapping("/account/address/create")
    public String createAddressPost(AccountAddress address,
                                    @RequestParam(required = false) String submitType,
                                    RedirectAttributes redirectAttributes) {
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            resetDefaultAddress(address.getAccountId());
        }
        accountAddressService.save(address);
        redirectAttributes.addFlashAttribute("success", "Thêm địa chỉ thành công.");
        if ("checkout".equalsIgnoreCase(submitType)) {
            return "redirect:/orders/checkout?accountId=" + address.getAccountId();
        }
        return "redirect:/account/address?accountId=" + address.getAccountId();
    }

    @GetMapping("/account/address/edit/{id}")
    public String editAddress(@PathVariable Integer id, Model model) {
        AccountAddress address = accountAddressService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + id));
        model.addAttribute("address", address);
        return "account/edit-address";
    }

    @PostMapping("/account/address/edit")
    public String editAddressPost(AccountAddress address,
                                  @RequestParam(required = false) String reloadFlag,
                                  RedirectAttributes redirectAttributes) {
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            resetDefaultAddress(address.getAccountId());
        }
        accountAddressService.save(address);
        redirectAttributes.addFlashAttribute("success", "Cập nhật địa chỉ thành công.");
        if (reloadFlag != null) {
            return "redirect:/orders/checkout?accountId=" + address.getAccountId();
        }
        return "redirect:/account/address?accountId=" + address.getAccountId();
    }

    @PostMapping("/account/address/delete/{id}")
    public String deleteAddress(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        AccountAddress address = accountAddressService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + id));
        Integer accountId = address.getAccountId();
        accountAddressService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Xóa địa chỉ thành công.");
        return "redirect:/account/address?accountId=" + accountId;
    }

    @PostMapping("/account/address/default/{id}")
    public String setDefaultAddress(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        AccountAddress address = accountAddressService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + id));
        resetDefaultAddress(address.getAccountId());
        address.setIsDefault(Boolean.TRUE);
        accountAddressService.save(address);
        redirectAttributes.addFlashAttribute("success", "Đã đặt địa chỉ mặc định.");
        return "redirect:/account/address?accountId=" + address.getAccountId();
    }

    @GetMapping("/account/tracking-order/{id}")
    public String trackingOrderDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("order", orderEntityService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id)));
        return "account/tracking-order-detail";
    }

    @GetMapping("/account/tracking-orders")
    public String trackingOrder(@RequestParam Integer accountId, Model model) {
        model.addAttribute("orders", orderEntityService.findByAccountId(accountId));
        return "account/tracking-order";
    }

    private void resetDefaultAddress(Integer accountId) {
        accountAddressService.findByAccountId(accountId).forEach(item -> {
            item.setIsDefault(Boolean.FALSE);
            accountAddressService.save(item);
        });
    }
}
