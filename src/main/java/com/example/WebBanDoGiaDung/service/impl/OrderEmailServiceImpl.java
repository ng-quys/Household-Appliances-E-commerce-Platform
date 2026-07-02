package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.controller.admin.AdminOrderController;
import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;
import com.example.WebBanDoGiaDung.entity.Delivery;
import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.OrderAddress;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.entity.Payment;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.service.OrderEmailService;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailServiceImpl implements OrderEmailService {

    private final OrderEntityService orderEntityService;
    private final OderDetailService oderDetailService;
    private final JavaMailSender mailSender;
    private final AdminOrderController adminOrderController;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Override
    @Transactional(readOnly = true)
    public void sendOrderEmail(OrderEmailEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Skip sending order email because event or orderId is missing.");
            return;
        }

        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("Mail skipped because mail config is missing. orderId={}, eventType={}", event.getOrderId(), event.getEventType());
            return;
        }

        OrderEntity order = orderEntityService.findByOrderId(event.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Skip sending order email because order not found. orderId={}", event.getOrderId());
            return;
        }

        String recipient = order.getAccount() != null ? order.getAccount().getEmail() : null;
        if (recipient == null || recipient.isBlank()) {
            log.warn("Mail skipped because recipient email is missing. orderId={}, eventType={}", event.getOrderId(), event.getEventType());
            return;
        }

        List<OderDetail> details = oderDetailService.findByOrderId(order.getOrderId());
        String subject = "ORDER_PAID".equalsIgnoreCase(event.getEventType())
                ? "Xác nhận thanh toán đơn hàng #" + order.getOrderId()
                : "Xác nhận đơn hàng #" + order.getOrderId();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(buildEmailHtml(order, details, event.getEventType()), true);
            mailSender.send(mimeMessage);
            log.info("Mail sent successfully. orderId={}, eventType={}, recipient={}",
                    order.getOrderId(),
                    event.getEventType(),
                    recipient);
        } catch (Exception exception) {
            log.error("Failed to send order confirmation email. orderId={}, eventType={}",
                    order.getOrderId(),
                    event.getEventType(),
                    exception);
        }
    }

    private String buildEmailHtml(OrderEntity order, List<OderDetail> details, String eventType) {
        Context context = new Context();
        context.setVariable("headline", "ORDER_PAID".equalsIgnoreCase(eventType)
                ? "Thanh toán đơn hàng thành công"
                : "Cảm ơn bạn đã đặt hàng");
        context.setVariable("orderCode", "#" + order.getOrderId());
        context.setVariable("customerName", adminOrderController.resolveCustomerName(order));
        context.setVariable("orderUrl", "http://localhost:8080/profile#order-history");
        context.setVariable("items", buildItemModels(details));
        context.setVariable("shippingAddress", resolveAddress(order.getOrderAddress()));
        context.setVariable("billingAddress", resolveAddress(order.getOrderAddress()));
        context.setVariable("deliveryMethod", adminOrderController.resolveDeliveryName(order));
        context.setVariable("paymentMethod", adminOrderController.resolvePaymentName(order));
        context.setVariable("subtotal", formatMoney(calculateSubtotal(details)));
        context.setVariable("shippingFee", formatMoney(resolveShippingFee(order.getDelivery()).doubleValue()));
        context.setVariable("grandTotal", adminOrderController.formatOrderTotal(order));
        context.setVariable("orderStatus", adminOrderController.resolveOrderStatus(order.getStatus()));
        return templateEngine.process("mail/order-confirmation", context);
    }

    private List<Map<String, Object>> buildItemModels(List<OderDetail> details) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OderDetail detail : details) {
            Product product = detail.getProduct();
            Map<String, Object> item = new HashMap<>();
            item.put("name", product != null && product.getProductName() != null ? product.getProductName() : "Sản phẩm");
            item.put("variant", resolveVariant(product));
            item.put("quantity", detail.getQuantity() != null ? detail.getQuantity() : 0);
            item.put("price", formatMoney(detail.getPrice() != null ? detail.getPrice() : 0D));
            item.put("imageUrl", resolvePublicImage(product));
            items.add(item);
        }
        return items;
    }

    private String resolveAddress(OrderAddress orderAddress) {
        if (orderAddress == null) {
            return "N/A";
        }
        String content = orderAddress.getContent() != null ? orderAddress.getContent().trim() : "";
        String ward = orderAddress.getWard() != null && orderAddress.getWard().getWardName() != null ? orderAddress.getWard().getWardName().trim() : "";
        String district = orderAddress.getDistrict() != null && orderAddress.getDistrict().getDistrictName() != null ? orderAddress.getDistrict().getDistrictName().trim() : "";
        String province = orderAddress.getProvince() != null && orderAddress.getProvince().getProvinceName() != null ? orderAddress.getProvince().getProvinceName().trim() : "";

        StringBuilder builder = new StringBuilder();
        appendPart(builder, content);
        appendPart(builder, ward);
        appendPart(builder, district);
        appendPart(builder, province);
        return builder.length() > 0 ? builder.toString() : "N/A";
    }

    private String resolveVariant(Product product) {
        if (product == null) {
            return "Phân loại: N/A";
        }
        String brandName = product.getBrand() != null && product.getBrand().getBrandName() != null
                ? product.getBrand().getBrandName().trim()
                : "N/A";
        String genreName = product.getGenre() != null && product.getGenre().getGenreName() != null
                ? product.getGenre().getGenreName().trim()
                : "N/A";
        return "Thương hiệu: " + brandName + " | Danh mục: " + genreName;
    }

    private String resolvePublicImage(Product product) {
        if (product == null || product.getImage() == null || product.getImage().isBlank()) {
            return null;
        }
        String image = product.getImage().trim();
        if (image.startsWith("http://") || image.startsWith("https://")) {
            return image;
        }
        return null;
    }

    private double calculateSubtotal(List<OderDetail> details) {
        double subtotal = 0D;
        for (OderDetail detail : details) {
            double price = detail.getPrice() != null ? detail.getPrice() : 0D;
            int quantity = detail.getQuantity() != null ? detail.getQuantity() : 0;
            subtotal += price * quantity;
        }
        return subtotal;
    }

    private BigDecimal resolveShippingFee(Delivery delivery) {
        return delivery != null && delivery.getPrice() != null ? delivery.getPrice() : BigDecimal.ZERO;
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f đ", amount).replace(',', '.');
    }

    private void appendPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value);
    }
}
