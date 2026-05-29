package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.security.AccountPrincipal;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountManagementController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AccountService accountService;
    private final OrderEntityService orderEntityService;

    public AdminAccountManagementController(AccountService accountService, OrderEntityService orderEntityService) {
        this.accountService = accountService;
        this.orderEntityService = orderEntityService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String search, Model model, Authentication authentication) {
        List<Account> users = accountService.findAll().stream()
                .filter(u -> search == null || search.isBlank()
                        || contains(u.getName(), search)
                        || contains(u.getEmail(), search)
                        || contains(u.getPhone(), search))
                .sorted(Comparator.comparing((Account u) -> u.getCreateAt() == null ? LocalDateTime.MIN : u.getCreateAt()).reversed())
                .toList();
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("currentAccountId", resolveCurrentAccountId(authentication));
        return "admin/account-management/index";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Integer id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Account user = accountService.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/admin/accounts";
        }

        Integer currentAccountId = resolveCurrentAccountId(authentication);
        if (currentAccountId != null && currentAccountId.equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không thể tự khóa chính tài khoản đang đăng nhập.");
            return "redirect:/admin/accounts";
        }

        if (isAdminRole(user.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Chưa cho phép khóa tài khoản ADMIN từ màn hình này.");
            return "redirect:/admin/accounts";
        }

        boolean locking = "1".equals(user.getStatus());
        user.setStatus(locking ? "0" : "1");
        touchAccount(user, authentication);
        accountService.save(user);
        redirectAttributes.addFlashAttribute("success", locking
                ? "Đã khóa tài khoản " + safeName(user)
                : "Đã mở khóa tài khoản " + safeName(user));
        return "redirect:/admin/accounts";
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Integer id,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        return updateStatus(id, "0", authentication, redirectAttributes);
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Integer id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        return updateStatus(id, "1", authentication, redirectAttributes);
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Integer id,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false, defaultValue = "") String sortOrder,
                          Model model,
                          Authentication authentication) {
        Account user = accountService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        List<OrderEntity> orders = orderEntityService.findByAccountId(id).stream()
                .filter(o -> search == null || search.isBlank() || String.valueOf(o.getOrderId()).contains(search))
                .sorted(switch (sortOrder) {
                    case "date_asc" -> Comparator.comparing(o -> o.getOrderDate() == null ? LocalDateTime.MIN : o.getOrderDate());
                    case "price_desc" -> Comparator.comparing((OrderEntity o) -> o.getTotal() == null ? 0D : o.getTotal()).reversed();
                    case "price_asc" -> Comparator.comparing(o -> o.getTotal() == null ? 0D : o.getTotal());
                    default -> Comparator.comparing((OrderEntity o) -> o.getOrderDate() == null ? LocalDateTime.MIN : o.getOrderDate()).reversed();
                })
                .toList();

        double totalSpent = orders.stream()
                .map(OrderEntity::getTotal)
                .filter(total -> total != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("search", search);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("currentAccountId", resolveCurrentAccountId(authentication));
        return "admin/account-management/details";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        return updateStatus(id, "0", authentication, redirectAttributes);
    }

    private String updateStatus(Integer id,
                                String targetStatus,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Account user = accountService.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/admin/accounts";
        }

        Integer currentAccountId = resolveCurrentAccountId(authentication);
        if (currentAccountId != null && currentAccountId.equals(id) && "0".equals(targetStatus)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không thể tự khóa chính tài khoản đang đăng nhập.");
            return "redirect:/admin/accounts";
        }

        if (isAdminRole(user.getRole()) && "0".equals(targetStatus)) {
            redirectAttributes.addFlashAttribute("error", "Chưa cho phép khóa tài khoản ADMIN từ màn hình này.");
            return "redirect:/admin/accounts";
        }

        user.setStatus(targetStatus);
        touchAccount(user, authentication);
        accountService.save(user);

        if ("1".equals(targetStatus)) {
            redirectAttributes.addFlashAttribute("success", "Đã mở khóa tài khoản " + safeName(user));
        } else {
            redirectAttributes.addFlashAttribute("success", "Đã khóa tài khoản " + safeName(user));
        }
        return "redirect:/admin/accounts";
    }

    private void touchAccount(Account user, Authentication authentication) {
        user.setUpdateAt(LocalDateTime.now());
        String actor = "admin";
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            if (principal.getEmail() != null && !principal.getEmail().isBlank()) {
                actor = principal.getEmail();
            }
        }
        user.setUpdateBy(actor);
    }

    private Integer resolveCurrentAccountId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return principal.getAccountId();
        }
        return null;
    }

    private boolean isAdminRole(Integer role) {
        return role != null && role == 0;
    }

    private String safeName(Account account) {
        if (account.getName() != null && !account.getName().isBlank()) {
            return account.getName();
        }
        return account.getEmail() != null ? account.getEmail() : "#" + account.getAccountId();
    }

    private boolean contains(String source, String search) {
        return source != null && search != null && source.toLowerCase().contains(search.toLowerCase());
    }
}
