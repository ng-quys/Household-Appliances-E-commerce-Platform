package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.AccountAddress;
import com.example.WebBanDoGiaDung.entity.Delivery;
import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.OrderAddress;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.entity.Payment;
import com.example.WebBanDoGiaDung.security.CurrentAccountService;
import com.example.WebBanDoGiaDung.service.AccountAddressService;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.DeliveryService;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.service.OrderAddressService;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import com.example.WebBanDoGiaDung.service.PaymentService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
@RequestMapping("/orders")
public class OrderController {

    private static final DateTimeFormatter INVOICE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AccountAddressService accountAddressService;
    private final AccountService accountService;
    private final DeliveryService deliveryService;
    private final PaymentService paymentService;
    private final OrderAddressService orderAddressService;
    private final OrderEntityService orderEntityService;
    private final OderDetailService oderDetailService;
    private final CurrentAccountService currentAccountService;

    public OrderController(AccountAddressService accountAddressService,
                           AccountService accountService,
                           DeliveryService deliveryService,
                           PaymentService paymentService,
                           OrderAddressService orderAddressService,
                           OrderEntityService orderEntityService,
                           OderDetailService oderDetailService,
                           CurrentAccountService currentAccountService) {
        this.accountAddressService = accountAddressService;
        this.accountService = accountService;
        this.deliveryService = deliveryService;
        this.paymentService = paymentService;
        this.orderAddressService = orderAddressService;
        this.orderEntityService = orderEntityService;
        this.oderDetailService = oderDetailService;
        this.currentAccountService = currentAccountService;
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam(required = false) Integer accountId, Model model) {
        List<AccountAddress> addresses = accountId == null ? List.of() : accountAddressService.findByAccountId(accountId);
        model.addAttribute("addresses", addresses);
        model.addAttribute("deliveries", deliveryService.findAll());
        model.addAttribute("payments", paymentService.findAll());
        return "order/checkout";
    }

    @PostMapping("/process")
    public String processOrder(@RequestParam Integer accountId,
                               @RequestParam Integer addressId,
                               @RequestParam Integer shippingMethod,
                               @RequestParam Integer paymentMethod,
                               @RequestParam(defaultValue = "0") Double total,
                               RedirectAttributes redirectAttributes) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        AccountAddress address = accountAddressService.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        Delivery delivery = deliveryService.findById(shippingMethod)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + shippingMethod));
        Payment payment = paymentService.findById(paymentMethod)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentMethod));

        OrderAddress orderAddress = new OrderAddress();
        orderAddress.setOrderUsername(truncate(address.getAccountUsername(), 20));
        orderAddress.setOrderPhoneNumber(truncate(address.getAccountPhoneNumber(), 10));
        orderAddress.setContent(truncate(address.getContent(), 150));
        orderAddress.setProvince(address.getProvince());
        orderAddress.setDistrict(address.getDistrict());
        orderAddress.setWard(address.getWard());
        orderAddress.setTimesEdit(0);
        orderAddressService.save(orderAddress);

        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setAccount(account);
        order.setOrderAddress(orderAddress);
        order.setDelivery(delivery);
        order.setPayment(payment);
        order.setOrderDate(now);
        order.setCreateAt(now);
        order.setUpdateAt(now);
        order.setStatus("0");
        order.setCreateBy("system");
        order.setUpdateBy("system");

        double finalTotal = total != null ? total : 0D;
        if (delivery.getPrice() != null) {
            finalTotal += delivery.getPrice().doubleValue();
        }
        order.setTotal(finalTotal);
        order = orderEntityService.save(order);

        redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công.");
        return "redirect:/orders/invoice/" + order.getOrderId() + "?created=true";
    }

    @GetMapping("/invoice/{orderId}")
    public String invoice(@PathVariable Integer orderId, Authentication authentication, Model model) {
        Account currentUser = currentAccountService.getCurrentAccount(authentication).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        OrderEntity order = orderEntityService.findByOrderId(orderId)
                .orElseGet(() -> orderEntityService.findById(orderId).orElse(null));
        if (order == null) {
            return "redirect:/profile/orders?error=order_not_found";
        }

        if (!isAdmin(authentication) && !isOwner(order, currentUser)) {
            return "redirect:/profile/orders?error=access_denied";
        }

        List<OderDetail> orderDetails = oderDetailService.findByOrderId(orderId);
        double subtotal = orderDetails.stream()
                .mapToDouble(detail -> (detail.getPrice() != null ? detail.getPrice() : 0D)
                        * (detail.getQuantity() != null ? detail.getQuantity() : 0))
                .sum();
        double total = order.getTotal() != null ? order.getTotal() : 0D;
        double shippingFee = 0D;
        if (order.getDelivery() != null && order.getDelivery().getPrice() != null) {
            shippingFee = order.getDelivery().getPrice().doubleValue();
        } else if (total >= subtotal) {
            shippingFee = total - subtotal;
        }

        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderDetails);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("total", total);
        model.addAttribute("invoiceDate", formatOrderDate(order));
        model.addAttribute("created", true);
        return "order/invoice";
    }

    @GetMapping("/success")
    public String success() {
        return "order/success";
    }

    public String formatOrderDate(OrderEntity order) {
        if (order == null || order.getOrderDate() == null) {
            return "";
        }
        return order.getOrderDate().format(INVOICE_DATE_FORMATTER);
    }

    public String resolveOrderStatus(String status) {
        if (status == null) {
            return "Chưa cập nhật";
        }
        return switch (status) {
            case "0" -> "Chờ thanh toán";
            case "1" -> "Chờ xác nhận";
            case "2" -> "Đang xử lý";
            case "3" -> "Đang giao hàng";
            case "4" -> "Hoàn thành";
            case "5" -> "Đã thanh toán";
            case "6" -> "Đã hủy";
            default -> status;
        };
    }

    public String resolveShippingAddress(OrderAddress orderAddress) {
        if (orderAddress == null) {
            return "Chưa cập nhật";
        }

        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, orderAddress.getContent());
        appendAddressPart(builder, orderAddress.getWard() != null ? orderAddress.getWard().getWardName() : null);
        appendAddressPart(builder, orderAddress.getDistrict() != null ? orderAddress.getDistrict().getDistrictName() : null);
        appendAddressPart(builder, orderAddress.getProvince() != null ? orderAddress.getProvince().getProvinceName() : null);

        return builder.length() > 0 ? builder.toString() : "Chưa cập nhật";
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }

    private boolean isOwner(OrderEntity order, Account currentUser) {
        if (order == null || currentUser == null || currentUser.getAccountId() == null) {
            return false;
        }
        if (order.getAccount() != null && order.getAccount().getAccountId() != null) {
            return currentUser.getAccountId().equals(order.getAccount().getAccountId());
        }
        return currentUser.getAccountId().equals(order.getAccountId());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
