package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.entity.AccountAddress;
import com.example.WebBanDoGiaDung.entity.Delivery;
import com.example.WebBanDoGiaDung.entity.Discount;
import com.example.WebBanDoGiaDung.entity.OrderAddress;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.entity.Payment;
import com.example.WebBanDoGiaDung.service.AccountAddressService;
import com.example.WebBanDoGiaDung.service.DeliveryService;
import com.example.WebBanDoGiaDung.service.DiscountService;
import com.example.WebBanDoGiaDung.service.OrderAddressService;
import com.example.WebBanDoGiaDung.service.OrderEntityService;
import com.example.WebBanDoGiaDung.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final AccountAddressService accountAddressService;
    private final DeliveryService deliveryService;
    private final PaymentService paymentService;
    private final DiscountService discountService;
    private final OrderAddressService orderAddressService;
    private final OrderEntityService orderEntityService;

    public OrderController(AccountAddressService accountAddressService,
                           DeliveryService deliveryService,
                           PaymentService paymentService,
                           DiscountService discountService,
                           OrderAddressService orderAddressService,
                           OrderEntityService orderEntityService) {
        this.accountAddressService = accountAddressService;
        this.deliveryService = deliveryService;
        this.paymentService = paymentService;
        this.discountService = discountService;
        this.orderAddressService = orderAddressService;
        this.orderEntityService = orderEntityService;
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
                               @RequestParam(required = false) String discountCode,
                               @RequestParam(defaultValue = "0") Double total,
                               RedirectAttributes redirectAttributes) {
        AccountAddress address = accountAddressService.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        Delivery delivery = deliveryService.findById(shippingMethod)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + shippingMethod));
        Payment payment = paymentService.findById(paymentMethod)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentMethod));

        Optional<Discount> discount = Optional.empty();
        if (discountCode != null && !discountCode.isBlank()) {
            discount = discountService.findByDiscountCode(discountCode);
        }

        OrderAddress orderAddress = new OrderAddress();
        orderAddress.setOrderUsername(address.getAccountUsername());
        orderAddress.setOrderPhoneNumber(address.getAccountPhoneNumber());
        orderAddress.setContent(address.getContent());
        orderAddressService.save(orderAddress);

        OrderEntity order = new OrderEntity();
        order.setAccountId(accountId);
        order.setOrderAddressId(orderAddress.getOrderAddressId());
        order.setDeliveryId(delivery.getDeliveryId());
        order.setPaymentId(payment.getPaymentId());
        order.setOrderDate(LocalDateTime.now());
        order.setCreateAt(LocalDateTime.now());
        order.setUpdateAt(LocalDateTime.now());
        order.setStatus("0");
        order.setCreateBy("system");
        order.setUpdateBy("system");

        double finalTotal = total;
        if (discount.isPresent() && discount.get().getDiscountPrice() != null) {
            finalTotal = Math.max(0D, total - discount.get().getDiscountPrice());
        }
        if (delivery.getPrice() != null) {
            finalTotal += delivery.getPrice().doubleValue();
        }
        order.setTotal(finalTotal);
        orderEntityService.save(order);

        redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công.");
        return "redirect:/orders/success";
    }

    @GetMapping("/success")
    public String success() {
        return "order/success";
    }
}
