package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.controller.CartController;
import com.example.WebBanDoGiaDung.dto.CartDto;
import com.example.WebBanDoGiaDung.dto.CartItemDto;
import com.example.WebBanDoGiaDung.dto.OrderEmailEvent;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.AccountAddress;
import com.example.WebBanDoGiaDung.entity.Delivery;
import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.OrderAddress;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.entity.Payment;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.entity.id.OderDetailId;
import com.example.WebBanDoGiaDung.service.AccountAddressService;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.CartService;
import com.example.WebBanDoGiaDung.service.CheckoutService;
import com.example.WebBanDoGiaDung.service.DeliveryService;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.service.OrderAddressService;
import com.example.WebBanDoGiaDung.service.OrderEmailPublisher;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import com.example.WebBanDoGiaDung.service.PaymentService;
import com.example.WebBanDoGiaDung.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private static final int ORDER_ADDRESS_CONTENT_MAX_LENGTH = 150;
    private static final int ORDER_USERNAME_MAX_LENGTH = 20;
    private static final int ORDER_PHONE_MAX_LENGTH = 10;

    private final AccountService accountService;
    private final AccountAddressService accountAddressService;
    private final DeliveryService deliveryService;
    private final PaymentService paymentService;
    private final OrderAddressService orderAddressService;
    private final OrderEntityService orderEntityService;
    private final OderDetailService oderDetailService;
    private final ProductService productService;
    private final CartService cartService;
    private final OrderEmailPublisher orderEmailPublisher;

    @Override
    @Transactional
    public OrderEntity createCodOrder(Integer accountId, Integer deliveryId, Integer accountAddressId, HttpSession session) {
        Payment codPayment = paymentService.findActiveByPaymentNames(List.of("cod", "COD", "Thanh toán khi nhận hàng"))
                .orElseThrow(() -> new IllegalArgumentException("missing_cod_payment"));
        OrderEntity order = createOrder(accountId, deliveryId, accountAddressId, session, codPayment, "1", "Thanh toán khi nhận hàng", "COD");
        session.removeAttribute(CartController.CART_SESSION_KEY);
        publishOrderCreatedEventSafely(order);
        return order;
    }

    @Override
    @Transactional
    public OrderEntity createBankTransferOrder(Integer accountId, Integer deliveryId, Integer accountAddressId, HttpSession session) {
        Payment momoPayment = paymentService.findActiveByPaymentName("momo")
                .orElseThrow(() -> new IllegalArgumentException("missing_momo_payment"));
        return createOrder(accountId, deliveryId, accountAddressId, session, momoPayment, "0", "Thanh toán MoMo", "MOMO_PENDING");
    }

    private OrderEntity createOrder(Integer accountId,
                                    Integer deliveryId,
                                    Integer accountAddressId,
                                    HttpSession session,
                                    Payment payment,
                                    String orderStatus,
                                    String orderNote,
                                    String transactionLabel) {
        CartDto cart = cartService.buildCart(session);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("empty_cart");
        }

        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("invalid_account"));

        Delivery delivery = deliveryService.findById(deliveryId)
                .filter(item -> "1".equals(item.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("invalid_delivery"));

        AccountAddress checkoutAddress = accountAddressService.getAddressForCheckout(accountId, accountAddressId);

        OrderAddress orderAddress = buildOrderAddress(account, checkoutAddress);
        orderAddress = orderAddressService.save(orderAddress);

        double shippingFee = delivery.getPrice() != null ? delivery.getPrice().doubleValue() : 0D;
        double subtotal = cart.getSubtotal() != null ? cart.getSubtotal() : 0D;
        double total = subtotal + shippingFee;

        OrderEntity order = buildOrderEntity(account, orderAddress, delivery, payment, total, orderStatus, orderNote);
        order = orderEntityService.save(order);

        for (CartItemDto item : cart.getItems()) {
            Product product = productService.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("invalid_product"));
            OderDetail detail = buildOrderDetail(order, product, item, transactionLabel);
            oderDetailService.save(detail);
        }

        return order;
    }

    private OrderAddress buildOrderAddress(Account account, AccountAddress checkoutAddress) {
        OrderAddress orderAddress = new OrderAddress();
        String receiverName = checkoutAddress.getAccountUsername() != null && !checkoutAddress.getAccountUsername().trim().isEmpty()
                ? checkoutAddress.getAccountUsername()
                : resolveReceiverName(account);
        String receiverPhone = checkoutAddress.getAccountPhoneNumber() != null && !checkoutAddress.getAccountPhoneNumber().trim().isEmpty()
                ? checkoutAddress.getAccountPhoneNumber()
                : resolveReceiverPhone(account);

        orderAddress.setOrderUsername(truncate(receiverName, ORDER_USERNAME_MAX_LENGTH));
        orderAddress.setOrderPhoneNumber(truncate(receiverPhone, ORDER_PHONE_MAX_LENGTH));
        orderAddress.setContent(truncate(checkoutAddress.getContent(), ORDER_ADDRESS_CONTENT_MAX_LENGTH));
        orderAddress.setProvince(checkoutAddress.getProvince());
        orderAddress.setDistrict(checkoutAddress.getDistrict());
        orderAddress.setWard(checkoutAddress.getWard());
        orderAddress.setTimesEdit(0);
        return orderAddress;
    }

    private OrderEntity buildOrderEntity(Account account,
                                         OrderAddress orderAddress,
                                         Delivery delivery,
                                         Payment payment,
                                         double total,
                                         String status,
                                         String orderNote) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setAccount(account);
        order.setOrderAddress(orderAddress);
        order.setDelivery(delivery);
        order.setPayment(payment);
        order.setOrderDate(now);
        order.setStatus(status);
        order.setOrderNote(orderNote);
        order.setCreateAt(now);
        order.setTotal(total);
        order.setCreateBy("system");
        order.setUpdateBy("system");
        order.setUpdateAt(now);
        return order;
    }

    private OderDetail buildOrderDetail(OrderEntity order, Product product, CartItemDto item, String transactionLabel) {
        LocalDateTime now = LocalDateTime.now();
        OderDetail detail = new OderDetail();
        detail.setId(new OderDetailId(
                product.getProductId(),
                product.getGenreId(),
                order.getOrderId()
        ));
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setPrice(item.getPrice());
        detail.setStatus("0");
        detail.setTransection(transactionLabel != null ? transactionLabel.toUpperCase(Locale.ROOT) : "PENDING");
        detail.setQuantity(item.getQuantity());
        detail.setCreateBy("system");
        detail.setCreateAt(now);
        detail.setUpdateBy("system");
        detail.setUpdateAt(now);
        return detail;
    }

    private void publishOrderCreatedEventSafely(OrderEntity order) {
        try {
            publishOrderCreatedEvent(order);
        } catch (Exception exception) {
            log.warn("Đơn hàng đã tạo thành công nhưng gửi email/queue thất bại. orderId={}",
                    order != null ? order.getOrderId() : null,
                    exception);
        }
    }

    private void publishOrderCreatedEvent(OrderEntity order) {
        if (order == null) {
            return;
        }
        OrderEmailEvent event = OrderEmailEvent.builder()
                .eventType("ORDER_CREATED")
                .orderId(order.getOrderId())
                .customerEmail(order.getAccount() != null ? order.getAccount().getEmail() : null)
                .customerName(order.getOrderAddress() != null ? order.getOrderAddress().getOrderUsername() : null)
                .paymentMethod(order.getPayment() != null ? order.getPayment().getPaymentName() : null)
                .orderStatus(order.getStatus())
                .total(order.getTotal() != null ? String.valueOf(order.getTotal()) : null)
                .orderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null)
                .build();
        orderEmailPublisher.publishOrderCreated(event);
    }

    private String resolveReceiverName(Account account) {
        if (account == null) {
            return "Khach hang";
        }
        if (account.getName() != null && !account.getName().trim().isEmpty()) {
            return account.getName().trim();
        }
        if (account.getEmail() != null && !account.getEmail().trim().isEmpty()) {
            return account.getEmail().trim();
        }
        return "Khach hang";
    }

    private String resolveReceiverPhone(Account account) {
        if (account == null || account.getPhone() == null) {
            return "";
        }
        return account.getPhone().trim();
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
