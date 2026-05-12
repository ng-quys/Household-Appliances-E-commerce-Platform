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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

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
    public OrderEntity createCodOrder(Integer accountId, Integer deliveryId, HttpSession session) {
        Payment codPayment = paymentService.findActiveByPaymentNames(List.of("cod", "COD", "Thanh toán khi nhận hàng"))
                .orElseThrow(() -> new IllegalArgumentException("missing_cod_payment"));
        OrderEntity order = createOrder(accountId, deliveryId, session, codPayment, "1", "Thanh toán khi nhận hàng", "COD");
        session.removeAttribute(CartController.CART_SESSION_KEY);
        publishOrderCreatedEvent(order);
        return order;
    }

    @Override
    @Transactional
    public OrderEntity createBankTransferOrder(Integer accountId, Integer deliveryId, HttpSession session) {
        Payment momoPayment = paymentService.findActiveByPaymentName("momo")
                .orElseThrow(() -> new IllegalArgumentException("missing_momo_payment"));
        return createOrder(accountId, deliveryId, session, momoPayment, "0", "Thanh toán MoMo", "MOMO_PENDING");
    }

    private OrderEntity createOrder(Integer accountId,
                                    Integer deliveryId,
                                    HttpSession session,
                                    Payment payment,
                                    String orderStatus,
                                    String orderNote,
                                    String transactionLabel) {
        CartDto cart = cartService.buildCart(session);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("empty_cart");
        }

        AccountAddress defaultAddress = accountAddressService.getDefaultAddress(accountId);
        Delivery delivery = deliveryService.findById(deliveryId)
                .filter(item -> "1".equals(item.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("invalid_delivery"));

        OrderAddress orderAddress = buildOrderAddress(defaultAddress);
        orderAddress = orderAddressService.save(orderAddress);

        double shippingFee = delivery.getPrice() != null ? delivery.getPrice().doubleValue() : 0D;
        double subtotal = cart.getSubtotal() != null ? cart.getSubtotal() : 0D;
        double total = subtotal + shippingFee;

        OrderEntity order = buildOrderEntity(accountId, orderAddress, delivery, payment, total, orderStatus, orderNote);
        order = orderEntityService.save(order);

        for (CartItemDto item : cart.getItems()) {
            Product product = productService.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("invalid_product"));
            OderDetail detail = buildOrderDetail(order, product, item, transactionLabel);
            oderDetailService.save(detail);
        }

        return order;
    }

    private OrderAddress buildOrderAddress(AccountAddress accountAddress) {
        OrderAddress orderAddress = new OrderAddress();
        orderAddress.setProvince(accountAddress.getProvince());
        orderAddress.setDistrict(accountAddress.getDistrict());
        orderAddress.setWard(accountAddress.getWard());
        orderAddress.setOrderUsername(accountAddress.getAccountUsername());
        orderAddress.setOrderPhoneNumber(accountAddress.getAccountPhoneNumber());
        orderAddress.setContent(accountAddress.getContent());
        orderAddress.setTimesEdit(0);
        return orderAddress;
    }

    private OrderEntity buildOrderEntity(Integer accountId,
                                         OrderAddress orderAddress,
                                         Delivery delivery,
                                         Payment payment,
                                         double total,
                                         String status,
                                         String orderNote) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        Account account = new Account();
        account.setAccountId(accountId);
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

    private OderDetail buildOrderDetail(OrderEntity order, Product product, CartItemDto item, String transactionLabel) {
        LocalDateTime now = LocalDateTime.now();
        OderDetail detail = new OderDetail();
        detail.setId(new OderDetailId(
                product.getProductId(),
                product.getGenreId(),
                product.getDisscountId(),
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
}
