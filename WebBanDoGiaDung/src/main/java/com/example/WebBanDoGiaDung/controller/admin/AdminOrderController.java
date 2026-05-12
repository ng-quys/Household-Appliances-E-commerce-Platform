package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.Delivery;
import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.OrderAddress;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.entity.Payment;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrderEntityService orderEntityService;
    private final OderDetailService oderDetailService;

    public AdminOrderController(OrderEntityService orderEntityService, OderDetailService oderDetailService) {
        this.orderEntityService = orderEntityService;
        this.oderDetailService = oderDetailService;
    }

    @GetMapping
    public String orderManagement(@RequestParam(required = false) String search,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false, defaultValue = "default") String sortOrder,
                                  Model model) {
        String normalizedSearch = search != null ? search.trim().toLowerCase(Locale.ROOT) : "";

        List<OrderEntity> orders = orderEntityService.findAllOrderByNewest().stream()
                .filter(order -> normalizedSearch.isBlank() || matchesSearch(order, normalizedSearch))
                .filter(order -> status == null || status.isBlank() || status.equals(order.getStatus()))
                .sorted(resolveSortComparator(sortOrder))
                .toList();

        model.addAttribute("orders", orders);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("sortOrder", sortOrder);
        return "admin/order/index";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Integer id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        OrderEntity order = orderEntityService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (!isValidStatus(status)) {
            redirectAttributes.addFlashAttribute("error", "Trạng thái đơn hàng không hợp lệ.");
            return "redirect:/admin/orders/" + id;
        }

        order.setStatus(status);
        order.setUpdateAt(LocalDateTime.now());
        order.setUpdateBy("admin");
        orderEntityService.save(order);
        redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái đơn hàng thành công.");
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("warning", "Không thể xóa đơn hàng, hãy cập nhật trạng thái hủy nếu cần.");
        return "redirect:/admin/orders";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Integer id, Model model) {
        OrderEntity order = orderEntityService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        List<OderDetail> details = oderDetailService.findByOrderId(id);

        model.addAttribute("order", order);
        model.addAttribute("details", details);
        model.addAttribute("statusOptions", List.of("0", "1", "2", "3", "4", "5", "6"));
        model.addAttribute("subtotal", details.stream()
                .mapToDouble(item -> safeDouble(item.getPrice()) * safeQuantity(item.getQuantity()))
                .sum());
        model.addAttribute("shippingFee", resolveShippingFee(order.getDelivery()));
        model.addAttribute("customerName", resolveCustomerName(order));
        model.addAttribute("customerEmail", resolveCustomerEmail(order));
        model.addAttribute("customerPhone", resolveCustomerPhone(order));
        model.addAttribute("fullAddress", resolveFullAddress(order.getOrderAddress()));
        return "admin/order/details";
    }

    public String resolveOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Không rõ";
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
            return "N/A";
        }
        if (order.getOrderDate() != null) {
            return order.getOrderDate().format(DATE_TIME_FORMATTER);
        }
        if (order.getCreateAt() != null) {
            return order.getCreateAt().format(DATE_TIME_FORMATTER);
        }
        return "N/A";
    }

    public String formatOrderTotal(OrderEntity order) {
        Double total = order != null ? order.getTotal() : null;
        double safeTotal = total != null ? total : 0D;
        return String.format(Locale.US, "%,.0f đ", safeTotal).replace(',', '.');
    }

    public String resolvePaymentName(OrderEntity order) {
        Payment payment = order != null ? order.getPayment() : null;
        return payment != null && payment.getPaymentName() != null && !payment.getPaymentName().isBlank()
                ? payment.getPaymentName()
                : "N/A";
    }

    public String resolveDeliveryName(OrderEntity order) {
        Delivery delivery = order != null ? order.getDelivery() : null;
        return delivery != null && delivery.getDeliveryName() != null && !delivery.getDeliveryName().isBlank()
                ? delivery.getDeliveryName()
                : "N/A";
    }

    public double resolveLineTotal(OderDetail detail) {
        return safeDouble(detail != null ? detail.getPrice() : null) * safeQuantity(detail != null ? detail.getQuantity() : null);
    }

    private Comparator<OrderEntity> resolveSortComparator(String sortOrder) {
        return switch (sortOrder) {
            case "date_asc" -> Comparator.comparing(order -> order.getOrderDate() == null ? LocalDateTime.MIN : order.getOrderDate());
            case "price_asc" -> Comparator.comparing(order -> order.getTotal() == null ? 0D : order.getTotal());
            case "price_desc" -> Comparator.comparing((OrderEntity order) -> order.getTotal() == null ? 0D : order.getTotal()).reversed();
            default -> Comparator.comparing((OrderEntity order) -> order.getOrderDate() == null ? LocalDateTime.MIN : order.getOrderDate()).reversed();
        };
    }

    private boolean matchesSearch(OrderEntity order, String search) {
        if (String.valueOf(order.getOrderId()).contains(search)) {
            return true;
        }
        return containsIgnoreCase(resolveCustomerName(order), search)
                || containsIgnoreCase(resolveCustomerEmail(order), search)
                || containsIgnoreCase(resolveCustomerPhone(order), search);
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private boolean isValidStatus(String status) {
        return List.of("0", "1", "2", "3", "4", "5", "6").contains(status);
    }

    public String resolveCustomerName(OrderEntity order) {
        OrderAddress orderAddress = order != null ? order.getOrderAddress() : null;
        if (orderAddress != null && orderAddress.getOrderUsername() != null && !orderAddress.getOrderUsername().isBlank()) {
            return orderAddress.getOrderUsername();
        }
        Account account = order != null ? order.getAccount() : null;
        if (account != null && account.getName() != null && !account.getName().isBlank()) {
            return account.getName();
        }
        return "Khách hàng";
    }

    public String resolveCustomerEmail(OrderEntity order) {
        Account account = order != null ? order.getAccount() : null;
        if (account != null && account.getEmail() != null && !account.getEmail().isBlank()) {
            return account.getEmail();
        }
        return "Chưa cập nhật";
    }

    public String resolveCustomerPhone(OrderEntity order) {
        OrderAddress orderAddress = order != null ? order.getOrderAddress() : null;
        if (orderAddress != null && orderAddress.getOrderPhoneNumber() != null && !orderAddress.getOrderPhoneNumber().isBlank()) {
            return orderAddress.getOrderPhoneNumber();
        }
        Account account = order != null ? order.getAccount() : null;
        if (account != null && account.getPhone() != null && !account.getPhone().isBlank()) {
            return account.getPhone();
        }
        return "Chưa cập nhật";
    }

    private String resolveFullAddress(OrderAddress orderAddress) {
        if (orderAddress == null) {
            return "Chưa cập nhật";
        }
        String content = safeText(orderAddress.getContent());
        String ward = orderAddress.getWard() != null ? safeText(orderAddress.getWard().getWardName()) : "";
        String district = orderAddress.getDistrict() != null ? safeText(orderAddress.getDistrict().getDistrictName()) : "";
        String province = orderAddress.getProvince() != null ? safeText(orderAddress.getProvince().getProvinceName()) : "";
        String location = String.join(", ", List.of(ward, district, province).stream().filter(item -> !item.isBlank()).toList());
        if (!content.isBlank() && !location.isBlank()) {
            return content + ", " + location;
        }
        return !content.isBlank() ? content : (!location.isBlank() ? location : "Chưa cập nhật");
    }

    private double resolveShippingFee(Delivery delivery) {
        BigDecimal price = delivery != null ? delivery.getPrice() : null;
        return price != null ? price.doubleValue() : 0D;
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0D;
    }

    private int safeQuantity(Integer value) {
        return value != null ? value : 0;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
