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
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.WebBanDoGiaDung.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrderEntityService orderEntityService;
    private final OderDetailService oderDetailService;
    private final ExcelExportService excelExportService;

    public AdminOrderController(OrderEntityService orderEntityService,
                                OderDetailService oderDetailService,
                                ExcelExportService excelExportService) {
        this.orderEntityService = orderEntityService;
        this.oderDetailService = oderDetailService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam(required = false) String search,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false, defaultValue = "default") String sortOrder) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, resolveSort(sortOrder));
        Page<OrderEntity> orderPage = orderEntityService.findAdminOrders(search, status, pageable);
        List<OrderEntity> orders = orderPage.getContent();

        java.io.ByteArrayInputStream in = excelExportService.exportOrders(orders);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=orders_report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }

    @GetMapping
    public String orderManagement(@RequestParam(required = false) String search,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false, defaultValue = "default") String sortOrder,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), resolvePageSize(size), resolveSort(sortOrder));
        Page<OrderEntity> orderPage = orderEntityService.findAdminOrders(search, status, pageable);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("size", pageable.getPageSize());
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

    private Sort resolveSort(String sortOrder) {
        return switch (sortOrder) {
            case "date_asc" -> Sort.by(Sort.Order.asc("orderDate"), Sort.Order.asc("orderId"));
            case "price_asc" -> Sort.by(Sort.Order.asc("total"), Sort.Order.desc("orderId"));
            case "price_desc" -> Sort.by(Sort.Order.desc("total"), Sort.Order.desc("orderId"));
            default -> Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("orderId"));
        };
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

    private int resolvePageSize(int size) {
        return size <= 0 ? 10 : Math.min(size, 100);
    }
}
