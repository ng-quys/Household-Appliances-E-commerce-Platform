package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.DistrictDto;
import com.example.WebBanDoGiaDung.dto.ProfileAddressForm;
import com.example.WebBanDoGiaDung.dto.ProfileUpdateForm;
import com.example.WebBanDoGiaDung.dto.WardDto;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.OrderAddress;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.security.AccountPrincipal;
import com.example.WebBanDoGiaDung.service.AccountAddressService;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.DistrictService;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import com.example.WebBanDoGiaDung.service.ProvinceService;
import com.example.WebBanDoGiaDung.service.WardService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
public class ProfileController {

    private final AccountAddressService accountAddressService;
    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProvinceService provinceService;
    private final DistrictService districtService;
    private final WardService wardService;
    private final OrderEntityService orderEntityService;
    private final OderDetailService oderDetailService;
    private final AccountService accountService;

    public ProfileController(AccountAddressService accountAddressService,
                             ProvinceService provinceService,
                             DistrictService districtService,
                             WardService wardService,
                             OrderEntityService orderEntityService,
                             OderDetailService oderDetailService,
                             AccountService accountService) {
        this.accountAddressService = accountAddressService;
        this.provinceService = provinceService;
        this.districtService = districtService;
        this.wardService = wardService;
        this.orderEntityService = orderEntityService;
        this.oderDetailService = oderDetailService;
        this.accountService = accountService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AccountPrincipal principal, Model model) {
        Account account = resolveCurrentAccount(principal);
        model.addAttribute("account", account);
        model.addAttribute("currentUser", account);
        model.addAttribute("displayName", resolveDisplayName(account));
        model.addAttribute("accountStatusLabel", resolveStatusLabel(account));
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", createProfileForm(account));
        }
        model.addAttribute("addressForm", new ProfileAddressForm());
        model.addAttribute("provinces", provinceService.findAllOrderByName());
        model.addAttribute("addresses", account != null
                ? accountAddressService.getAddressesByCurrentAccount(account.getAccountId())
                : List.of());
        List<OrderEntity> orders = account != null
                ? orderEntityService.findByAccountId(account.getAccountId())
                : List.of();
        model.addAttribute("orders", orders);
        model.addAttribute("orderDetailsMap", buildOrderDetailsMap(orders));
        return "auth/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal AccountPrincipal principal,
                                @ModelAttribute("profileForm") ProfileUpdateForm profileForm,
                                RedirectAttributes redirectAttributes) {
        if (principal == null || principal.getAccount() == null) {
            return "redirect:/login";
        }

        String normalizedName = profileForm.getName() != null ? profileForm.getName().trim() : "";
        String normalizedPhone = profileForm.getPhone() != null ? profileForm.getPhone().trim() : "";

        if (normalizedName.isEmpty()) {
            redirectAttributes.addFlashAttribute("profileError", "Họ tên không được để trống.");
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            return "redirect:/profile#account-info";
        }

        if (normalizedPhone.isEmpty()) {
            redirectAttributes.addFlashAttribute("profileError", "Số điện thoại không được để trống.");
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            return "redirect:/profile#account-info";
        }

        if (!normalizedPhone.matches("\\d{9,11}")) {
            redirectAttributes.addFlashAttribute("profileError", "Số điện thoại phải gồm 9 đến 11 chữ số.");
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            return "redirect:/profile#account-info";
        }

        Account account = resolveCurrentAccount(principal);
        if (account == null) {
            return "redirect:/login";
        }

        account.setName(normalizedName);
        account.setPhone(normalizedPhone);
        account.setUpdateAt(LocalDateTime.now());
        accountService.save(account);

        redirectAttributes.addFlashAttribute("profileSuccess", "Cập nhật thông tin tài khoản thành công.");
        return "redirect:/profile#account-info";
    }

    @PostMapping("/profile/address/add")
    public String addAddress(@AuthenticationPrincipal AccountPrincipal principal,
                             ProfileAddressForm addressForm,
                             RedirectAttributes redirectAttributes) {
        return handleAddressAction(principal, redirectAttributes, () ->
                accountAddressService.addAddress(principal.getAccount().getAccountId(), addressForm));
    }

    @PostMapping("/profile/address/update")
    public String updateAddress(@AuthenticationPrincipal AccountPrincipal principal,
                                ProfileAddressForm addressForm,
                                RedirectAttributes redirectAttributes) {
        return handleAddressAction(principal, redirectAttributes, () ->
                accountAddressService.updateAddress(principal.getAccount().getAccountId(), addressForm));
    }

    @PostMapping("/profile/address/delete")
    public String deleteAddress(@AuthenticationPrincipal AccountPrincipal principal,
                                @RequestParam Integer addressId,
                                RedirectAttributes redirectAttributes) {
        return handleAddressAction(principal, redirectAttributes, () -> {
            accountAddressService.deleteAddress(principal.getAccount().getAccountId(), addressId);
            return null;
        });
    }

    @PostMapping("/profile/address/default")
    public String setDefaultAddress(@AuthenticationPrincipal AccountPrincipal principal,
                                    @RequestParam Integer addressId,
                                    RedirectAttributes redirectAttributes) {
        return handleAddressAction(principal, redirectAttributes, () -> {
            accountAddressService.setDefaultAddress(principal.getAccount().getAccountId(), addressId);
            return null;
        });
    }

    @GetMapping("/api/address/districts")
    public ResponseEntity<List<DistrictDto>> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(districtService.findByProvinceId(provinceId).stream()
                .map(district -> new DistrictDto(district.getDistrictId(), district.getDistrictName()))
                .toList());
    }

    @GetMapping("/api/address/wards")
    public ResponseEntity<List<WardDto>> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(wardService.findByDistrictId(districtId).stream()
                .map(ward -> new WardDto(ward.getWardId(), ward.getWardName()))
                .toList());
    }

    @GetMapping("/user")
    public String userHome() {
        return "redirect:/profile";
    }

    private String handleAddressAction(AccountPrincipal principal,
                                       RedirectAttributes redirectAttributes,
                                       AddressAction action) {
        if (principal == null || principal.getAccount() == null) {
            return "redirect:/login";
        }
        try {
            action.execute();
            redirectAttributes.addFlashAttribute("addressSuccess", "Cập nhật sổ địa chỉ thành công.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("addressError", exception.getMessage());
        }
        return "redirect:/profile#address-book";
    }

    private String resolveDisplayName(Account account) {
        if (account == null) {
            return "Tài khoản";
        }
        String name = account.getName() != null ? account.getName().trim() : "";
        if (!name.isEmpty()) {
            return name;
        }
        String email = account.getEmail() != null ? account.getEmail().trim() : "";
        if (!email.isEmpty()) {
            return email;
        }
        return "Tài khoản";
    }

    private String resolveStatusLabel(Account account) {
        if (account == null || account.getStatus() == null) {
            return "Chưa xác định";
        }
        return "1".equals(account.getStatus()) ? "Đang hoạt động" : "Tạm khóa";
    }

    public String resolveOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Đang xử lý";
        }
        return switch (status) {
            case "0" -> "Chờ thanh toán";
            case "1" -> "Chờ xác nhận";
            case "2" -> "Đang chuẩn bị hàng";
            case "3" -> "Đang giao";
            case "4" -> "Hoàn thành";
            case "5" -> "Đã thanh toán";
            case "6" -> "Đã hủy";
            default -> status;
        };
    }

    public String formatOrderDate(OrderEntity order) {
        if (order == null) {
            return "Chưa cập nhật";
        }
        if (order.getOrderDate() != null) {
            return order.getOrderDate().format(ORDER_DATE_FORMATTER);
        }
        if (order.getCreateAt() != null) {
            return order.getCreateAt().format(ORDER_DATE_FORMATTER);
        }
        return "Chưa cập nhật";
    }

    public String resolvePaymentName(OrderEntity order) {
        if (order != null && order.getPayment() != null && order.getPayment().getPaymentName() != null
                && !order.getPayment().getPaymentName().isBlank()) {
            return order.getPayment().getPaymentName();
        }
        return "Chưa cập nhật";
    }

    public String resolveDeliveryName(OrderEntity order) {
        if (order != null && order.getDelivery() != null && order.getDelivery().getDeliveryName() != null
                && !order.getDelivery().getDeliveryName().isBlank()) {
            return order.getDelivery().getDeliveryName();
        }
        return "Chưa cập nhật";
    }

    public String resolveFullOrderAddress(OrderEntity order) {
        if (order == null) {
            return "Chưa cập nhật";
        }
        OrderAddress orderAddress = order.getOrderAddress();
        if (orderAddress == null) {
            return "Chưa cập nhật";
        }
        String content = safeText(orderAddress.getContent());
        String recipient = safeText(orderAddress.getOrderUsername());
        String phone = safeText(orderAddress.getOrderPhoneNumber());
        String summary = content;
        if (!recipient.isEmpty() || !phone.isEmpty()) {
            summary += " (" + recipient + (recipient.isEmpty() || phone.isEmpty() ? "" : " - ") + phone + ")";
        }
        return summary.isBlank() ? "Chưa cập nhật" : summary;
    }

    public String resolveProductSummary(OderDetail detail) {
        if (detail == null || detail.getProduct() == null) {
            return "Sản phẩm";
        }
        String productName = safeText(detail.getProduct().getProductName());
        String brandName = detail.getProduct().getBrand() != null ? safeText(detail.getProduct().getBrand().getBrandName()) : "";
        String genreName = detail.getProduct().getGenre() != null ? safeText(detail.getProduct().getGenre().getGenreName()) : "";
        String suffix = "";
        if (!brandName.isEmpty() || !genreName.isEmpty()) {
            suffix = " (" + brandName + (!brandName.isEmpty() && !genreName.isEmpty() ? " / " : "") + genreName + ")";
        }
        return (productName.isEmpty() ? "Sản phẩm" : productName) + suffix;
    }

    private Map<Integer, List<OderDetail>> buildOrderDetailsMap(List<OrderEntity> orders) {
        Map<Integer, List<OderDetail>> detailsMap = new LinkedHashMap<>();
        for (OrderEntity order : orders) {
            if (order != null && order.getOrderId() != null) {
                detailsMap.put(order.getOrderId(), oderDetailService.findByOrderId(order.getOrderId()));
            }
        }
        return detailsMap;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    private ProfileUpdateForm createProfileForm(Account account) {
        ProfileUpdateForm form = new ProfileUpdateForm();
        if (account != null) {
            form.setName(account.getName());
            form.setPhone(account.getPhone());
        }
        return form;
    }

    private Account resolveCurrentAccount(AccountPrincipal principal) {
        if (principal == null || principal.getAccount() == null) {
            return null;
        }
        Integer accountId = principal.getAccount().getAccountId();
        if (accountId != null) {
            return accountService.findById(accountId).orElse(null);
        }
        String email = principal.getUsername();
        if (email != null && !email.isBlank()) {
            return accountService.findByEmail(email.trim()).orElse(null);
        }
        return null;
    }

    @FunctionalInterface
    private interface AddressAction {
        Object execute();
    }
}
