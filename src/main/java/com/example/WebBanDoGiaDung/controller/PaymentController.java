package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;
import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.service.*;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final OrderEntityService orderEntityService;
    private final MomoService momoService;
    private final OrderEmailPublisher orderEmailPublisher;
    private final OderDetailService oderDetailService;

    public PaymentController(OrderEntityService orderEntityService,
                             MomoService momoService,
                             OrderEmailPublisher orderEmailPublisher,
                             OderDetailService oderDetailService) {
        this.orderEntityService = orderEntityService;
        this.momoService = momoService;
        this.orderEmailPublisher = orderEmailPublisher;
        this.oderDetailService = oderDetailService;
    }

    @GetMapping("/momo/create")
    public String createMomoPayment(@RequestParam Integer orderId, Model model) {
        Optional<OrderEntity> orderOptional = orderEntityService.findById(orderId);
        if (orderOptional.isEmpty()) {
            return "redirect:/cart?error=order_not_found";
        }

        OrderEntity order = orderOptional.get();
        BigDecimal amount = BigDecimal.valueOf(order.getTotal() != null ? order.getTotal() : 0D);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "redirect:/cart?error=invalid_amount";
        }

        try {
            String payUrl = momoService.createPayment(amount, orderId);
            return "redirect:" + payUrl;
        } catch (MomoPaymentException exception) {
            model.addAttribute("success", false);
            model.addAttribute("title", "Thanh toán MoMo thất bại");
            model.addAttribute("message", exception.getMessage());
            model.addAttribute("orderId", exception.getOrderId() != null ? exception.getOrderId() : String.valueOf(orderId));
            model.addAttribute("amount", amount.toPlainString());
            model.addAttribute("resultCode", exception.getResultCode() != null ? exception.getResultCode() : "UNKNOWN");
            model.addAttribute("responseBody", exception.getResponseBody());
            model.addAttribute("localMessage", exception.getLocalMessage());
            model.addAttribute("requestId", exception.getRequestId());
            model.addAttribute("momoOrderId", exception.getMomoOrderId());
            model.addAttribute("momoTransactionId", exception.getMomoTransactionId());
            return "payment/result";
        }
    }

    @GetMapping("/momo/return")
    public String returnUrl(@RequestParam Map<String, String> params, HttpSession session, Model model) {
        String resultCode = params.getOrDefault("resultCode", "-1");
        String momoOrderId = params.getOrDefault("orderId", "");
        String extraData = params.getOrDefault("extraData", "");
        String amount = params.getOrDefault("amount", "0");
        String message = params.getOrDefault("message", "Thanh toán không thành công.");
        String transId = params.getOrDefault("transId", "");
        Integer internalOrderId = momoService.resolveInternalOrderId(momoOrderId, extraData);

        boolean success = "0".equals(resultCode);
        if (success && internalOrderId != null) {
            boolean updated = tryUpdateOrderStatus(String.valueOf(internalOrderId), "5");
            session.removeAttribute(CartController.CART_SESSION_KEY);
            if (updated) {
                publishOrderPaidEvent(internalOrderId);
            }
            return "redirect:/orders/invoice/" + internalOrderId + "?paid=true";
        }

        model.addAttribute("success", success);
        model.addAttribute("title", success ? "Thanh toán MoMo thành công" : "Thanh toán MoMo thất bại");
        model.addAttribute("message", success ? "Đơn hàng của bạn đã được ghi nhận thanh toán thành công." : message);
        model.addAttribute("orderId", internalOrderId != null ? internalOrderId : momoOrderId);
        model.addAttribute("amount", amount);
        model.addAttribute("resultCode", resultCode);
        model.addAttribute("momoOrderId", momoOrderId);
        model.addAttribute("momoTransactionId", transId);

        List<OderDetail> orderDetails = internalOrderId != null
                ? oderDetailService.findByOrderId(internalOrderId)
                : List.of();
        model.addAttribute("orderDetails", orderDetails);

        return "payment/result";
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Map<String, Object>> ipn(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, String> payload = new LinkedHashMap<>();
        if (body != null) {
            body.forEach((key, value) -> payload.put(key, value != null ? String.valueOf(value) : ""));
        }

        boolean validSignature = momoService.verifyIpnSignature(payload);
        String resultCode = payload.getOrDefault("resultCode", "-1");
        if (validSignature && "0".equals(resultCode)) {
            Integer internalOrderId = momoService.resolveInternalOrderId(payload.get("orderId"), payload.get("extraData"));
            if (internalOrderId != null) {
                tryUpdateOrderStatus(String.valueOf(internalOrderId), "5");
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "signatureValid", validSignature,
                "note", validSignature ? "IPN processed" : "Invalid signature"
        ));
    }

    private boolean tryUpdateOrderStatus(String orderIdValue, String status) {
        try {
            Integer orderId = Integer.valueOf(orderIdValue);
            OrderEntity order = orderEntityService.findById(orderId).orElse(null);
            if (order == null) {
                return false;
            }
            if (status.equals(order.getStatus())) {
                return false;
            }
            order.setStatus(status);
            orderEntityService.update(orderId, order);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void publishOrderPaidEvent(Integer orderId) {
        OrderEntity order = orderEntityService.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        OrderEmailEvent event = OrderEmailEvent.builder()
                .eventType("ORDER_PAID")
                .orderId(order.getOrderId())
                .customerEmail(order.getAccount() != null ? order.getAccount().getEmail() : null)
                .customerName(order.getOrderAddress() != null ? order.getOrderAddress().getOrderUsername() : null)
                .paymentMethod(order.getPayment() != null ? order.getPayment().getPaymentName() : null)
                .orderStatus(order.getStatus())
                .total(order.getTotal() != null ? String.valueOf(order.getTotal()) : null)
                .orderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null)
                .build();
        orderEmailPublisher.publishOrderPaid(event);
    }
}
