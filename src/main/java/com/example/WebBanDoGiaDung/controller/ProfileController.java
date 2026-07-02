package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.DistrictDto;
import com.example.WebBanDoGiaDung.dto.ProfileAddressForm;
import com.example.WebBanDoGiaDung.dto.ProfileUpdateForm;
import com.example.WebBanDoGiaDung.dto.WardDto;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.OrderAddress;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.security.CurrentAccountService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ProfileController {

    private static final DateTimeFormatter ORDER_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CurrentAccountService currentAccountService;
    private final AccountAddressService accountAddressService;
    private final ProvinceService provinceService;
    private final DistrictService districtService;
    private final WardService wardService;
    private final OrderEntityService orderEntityService;
    private final OderDetailService oderDetailService;
    private final AccountService accountService;

    @GetMapping("/profile")
    public String profile() {
        return "redirect:/profile/account";
    }

    @GetMapping("/profile/account")
    public String account(Authentication authentication, Model model) {
        Account account = getAuthenticatedAccount(authentication);

        if (account == null) {
            return "redirect:/login";
        }

        addCommonProfileAttributes(model, account, "account");

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", createProfileForm(account));
        }

        return "auth/profile-account";
    }

    @GetMapping("/profile/orders")
    public String orders(Authentication authentication,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "5") int size,
                         Model model) {
        Account account = getAuthenticatedAccount(authentication);

        if (account == null) {
            return "redirect:/login";
        }

        addCommonProfileAttributes(model, account, "orders");

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 10));
        Page<OrderEntity> orderPage = orderEntityService.findPageByAccountId(account.getAccountId(), pageable);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("orderDetailsMap", buildOrderDetailsMap(orderPage.getContent()));

        return "auth/profile-orders";
    }

    @GetMapping("/profile/addresses")
    public String addresses(Authentication authentication,
                            @RequestParam(required = false) String addressRequired,
                            Model model) {
        Account account = getAuthenticatedAccount(authentication);

        if (account == null) {
            return "redirect:/login";
        }

        addCommonProfileAttributes(model, account, "addresses");

        if ("true".equals(addressRequired) && !model.containsAttribute("addressError")) {
            model.addAttribute("addressError", "Bạn cần thêm địa chỉ giao hàng hợp lệ trước khi đặt hàng.");
        }

        model.addAttribute("addressForm", new ProfileAddressForm());
        model.addAttribute("provinces", provinceService.findAllOrderByName());
        model.addAttribute("addresses",
                accountAddressService.getAddressViewsByCurrentAccount(account.getAccountId()));

        return "auth/profile-addresses";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @ModelAttribute("profileForm") ProfileUpdateForm profileForm,
                                RedirectAttributes redirectAttributes) {
        Account account = getAuthenticatedAccount(authentication);

        if (account == null) {
            return "redirect:/login";
        }

        String normalizedName = profileForm.getName() != null ? profileForm.getName().trim() : "";
        String normalizedPhone = profileForm.getPhone() != null ? profileForm.getPhone().trim() : "";

        if (normalizedName.isEmpty()) {
            redirectAttributes.addFlashAttribute("profileError", "Họ tên không được để trống.");
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            return "redirect:/profile/account";
        }

        if (normalizedPhone.isEmpty()) {
            redirectAttributes.addFlashAttribute("profileError", "Số điện thoại không được để trống.");
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            return "redirect:/profile/account";
        }

        if (!normalizedPhone.matches("\\d{9,11}")) {
            redirectAttributes.addFlashAttribute("profileError", "Số điện thoại phải gồm 9 đến 11 chữ số.");
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            return "redirect:/profile/account";
        }

        account.setName(normalizedName);
        account.setPhone(normalizedPhone);
        account.setUpdateAt(LocalDateTime.now());
        accountService.save(account);

        redirectAttributes.addFlashAttribute("profileSuccess", "Cập nhật thông tin tài khoản thành công.");
        return "redirect:/profile/account";
    }

    @PostMapping("/profile/address/add")
    public String addAddress(Authentication authentication,
                             ProfileAddressForm addressForm,
                             RedirectAttributes redirectAttributes) {
        Account account = getAuthenticatedAccount(authentication);

        return handleAddressAction(account, redirectAttributes, () ->
                accountAddressService.addAddress(account.getAccountId(), addressForm));
    }

    @PostMapping("/profile/address/update")
    public String updateAddress(Authentication authentication,
                                ProfileAddressForm addressForm,
                                RedirectAttributes redirectAttributes) {
        Account account = getAuthenticatedAccount(authentication);

        return handleAddressAction(account, redirectAttributes, () ->
                accountAddressService.updateAddress(account.getAccountId(), addressForm));
    }

    @PostMapping("/profile/address/delete")
    public String deleteAddress(Authentication authentication,
                                @RequestParam Integer addressId,
                                RedirectAttributes redirectAttributes) {
        Account account = getAuthenticatedAccount(authentication);

        return handleAddressAction(account, redirectAttributes, () -> {
            accountAddressService.deleteAddress(account.getAccountId(), addressId);
            return null;
        });
    }

    @PostMapping("/profile/address/default")
    public String setDefaultAddress(Authentication authentication,
                                    @RequestParam Integer addressId,
                                    RedirectAttributes redirectAttributes) {
        Account account = getAuthenticatedAccount(authentication);

        return handleAddressAction(account, redirectAttributes, () -> {
            accountAddressService.setDefaultAddress(account.getAccountId(), addressId);
            return null;
        });
    }

    @GetMapping("/api/address/districts")
    public ResponseEntity<List<DistrictDto>> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(districtService.findByProvinceId(provinceId)
                .stream()
                .map(district -> new DistrictDto(district.getDistrictId(), district.getDistrictName()))
                .toList());
    }

    @GetMapping("/api/address/wards")
    public ResponseEntity<List<WardDto>> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(wardService.findByDistrictId(districtId)
                .stream()
                .map(ward -> new WardDto(ward.getWardId(), ward.getWardName()))
                .toList());
    }

    @GetMapping("/user")
    public String userHome() {
        return "redirect:/profile/account";
    }

    private String handleAddressAction(Account account,
                                       RedirectAttributes redirectAttributes,
                                       AddressAction action) {
        if (account == null) {
            return "redirect:/login";
        }

        try {
            action.execute();
            redirectAttributes.addFlashAttribute("addressSuccess", "Cập nhật sổ địa chỉ thành công.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("addressError", exception.getMessage());
        }

        return "redirect:/profile/addresses";
    }

    private Account getAuthenticatedAccount(Authentication authentication) {
        return currentAccountService.getCurrentAccount(authentication).orElse(null);
    }

    private void addCommonProfileAttributes(Model model, Account account, String activeTab) {
        model.addAttribute("account", account);
        model.addAttribute("currentUser", account);
        model.addAttribute("displayName", resolveDisplayName(account));
        model.addAttribute("accountStatusLabel", resolveStatusLabel(account));
        model.addAttribute("activeTab", activeTab);
    }

    private String resolveDisplayName(Account account) {
        if (account == null) {
            return "Tài khoản";
        }

        String name = safeText(account.getName());
        if (!name.isEmpty()) {
            return name;
        }

        String email = safeText(account.getEmail());
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
        if (order != null
                && order.getPayment() != null
                && order.getPayment().getPaymentName() != null
                && !order.getPayment().getPaymentName().isBlank()) {
            return order.getPayment().getPaymentName();
        }

        return "Chưa cập nhật";
    }

    public String resolveDeliveryName(OrderEntity order) {
        if (order != null
                && order.getDelivery() != null
                && order.getDelivery().getDeliveryName() != null
                && !order.getDelivery().getDeliveryName().isBlank()) {
            return order.getDelivery().getDeliveryName();
        }

        return "Chưa cập nhật";
    }

    public String resolveFullOrderAddress(OrderEntity order) {
        if (order == null || order.getOrderAddress() == null) {
            return "Chưa cập nhật";
        }

        OrderAddress orderAddress = order.getOrderAddress();

        String content = safeText(orderAddress.getContent());
        String recipient = safeText(orderAddress.getOrderUsername());
        String phone = safeText(orderAddress.getOrderPhoneNumber());

        String summary = content;

        if (!recipient.isEmpty() || !phone.isEmpty()) {
            summary += " (" + recipient
                    + (recipient.isEmpty() || phone.isEmpty() ? "" : " - ")
                    + phone + ")";
        }

        return summary.isBlank() ? "Chưa cập nhật" : summary;
    }

    public String resolveProductSummary(OderDetail detail) {
        if (detail == null || detail.getProduct() == null) {
            return "Sản phẩm";
        }

        String productName = safeText(detail.getProduct().getProductName());

        String brandName = detail.getProduct().getBrand() != null
                ? safeText(detail.getProduct().getBrand().getBrandName())
                : "";

        String genreName = detail.getProduct().getGenre() != null
                ? safeText(detail.getProduct().getGenre().getGenreName())
                : "";

        String suffix = "";

        if (!brandName.isEmpty() || !genreName.isEmpty()) {
            suffix = " (" + brandName
                    + (!brandName.isEmpty() && !genreName.isEmpty() ? " / " : "")
                    + genreName + ")";
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

    private ProfileUpdateForm createProfileForm(Account account) {
        ProfileUpdateForm form = new ProfileUpdateForm();

        if (account != null) {
            form.setName(account.getName());
            form.setPhone(account.getPhone());
        }

        return form;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    @FunctionalInterface
    private interface AddressAction {
        Object execute();
    }
}