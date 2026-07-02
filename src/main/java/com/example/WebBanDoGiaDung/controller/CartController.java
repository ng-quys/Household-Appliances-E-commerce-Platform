package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.CartDto;
import com.example.WebBanDoGiaDung.dto.ProfileAddressView;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.Delivery;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.security.CurrentAccountService;
import com.example.WebBanDoGiaDung.service.AccountAddressService;
import com.example.WebBanDoGiaDung.service.CartService;
import com.example.WebBanDoGiaDung.service.CheckoutService;
import com.example.WebBanDoGiaDung.service.DeliveryService;
import com.example.WebBanDoGiaDung.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/cart")
public class CartController {

    public static final String CART_SESSION_KEY = "CART";
    private final ProductService productService;
    private final DeliveryService deliveryService;
    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final AccountAddressService accountAddressService;
    private final CurrentAccountService currentAccountService;

    public CartController(ProductService productService,
                          DeliveryService deliveryService,
                          CheckoutService checkoutService,
                          CartService cartService,
                          AccountAddressService accountAddressService,
                          CurrentAccountService currentAccountService) {
        this.productService = productService;
        this.deliveryService = deliveryService;
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.accountAddressService = accountAddressService;
        this.currentAccountService = currentAccountService;
    }

    @GetMapping
    public String index(HttpSession session, Model model, Authentication authentication) {
        getCartMap(session); // chuẩn hóa lại giỏ hàng cũ trong session trước khi render
        CartDto cart = cartService.buildCart(session);
        List<Delivery> deliveries = deliveryService.findByStatus("1");
        Delivery selectedDelivery = deliveries.isEmpty() ? null : deliveries.get(0);
        double shippingFee = selectedDelivery != null && selectedDelivery.getPrice() != null
                ? selectedDelivery.getPrice().doubleValue()
                : 0D;

        model.addAttribute("cart", cart);
        model.addAttribute("deliveries", deliveries);
        model.addAttribute("selectedDeliveryId", selectedDelivery != null ? selectedDelivery.getDeliveryId() : null);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("grandTotal", (cart.getSubtotal() != null ? cart.getSubtotal() : 0D) + shippingFee);
        List<ProfileAddressView> shippingAddresses = Collections.emptyList();
        Integer selectedAccountAddressId = null;
        Account currentUser = currentAccountService.getCurrentAccount(authentication).orElse(null);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            shippingAddresses = accountAddressService.getAddressViewsByCurrentAccount(currentUser.getAccountId());
            ProfileAddressView selectedAddress = null;
            for (ProfileAddressView address : shippingAddresses) {
                if (Boolean.TRUE.equals(address.getIsDefault())) {
                    selectedAddress = address;
                    break;
                }
            }
            if (selectedAddress == null && !shippingAddresses.isEmpty()) {
                selectedAddress = shippingAddresses.get(0);
            }
            selectedAccountAddressId = selectedAddress != null ? selectedAddress.getAccountAddressId() : null;
        }

        model.addAttribute("shippingAddresses", shippingAddresses);
        if (!model.containsAttribute("selectedAccountAddressId")) {
            model.addAttribute("selectedAccountAddressId", selectedAccountAddressId);
        }

        return "cart/index";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Integer productId,
                            @RequestParam(required = false) String redirect,
                            HttpSession session,
                            HttpServletRequest request) {
        Map<Integer, Integer> cart = getCartMap(session);
        cart.merge(productId, 1, Integer::sum);
        session.setAttribute(CART_SESSION_KEY, cart);

        if (redirect != null && !redirect.isBlank()) {
            return "redirect:" + redirect;
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isBlank() ? referer : "/cart");
    }

    @PostMapping("/add-ajax")
    @ResponseBody
    public Map<String, Object> addToCartAjax(@RequestParam Integer productId,
                                             HttpSession session) {
        Map<Integer, Integer> cart = getCartMap(session);
        cart.merge(productId, 1, Integer::sum);
        session.setAttribute(CART_SESSION_KEY, cart);

        return Map.of(
                "success", true,
                "cartCount", cartService.getCartQuantity(session),
                "message", "Đã thêm sản phẩm vào giỏ hàng"
        );
    }

    @PostMapping("/increase")
    public String increase(@RequestParam Integer productId, HttpSession session) {
        Map<Integer, Integer> cart = getCartMap(session);
        cart.computeIfPresent(productId, (id, quantity) -> quantity + 1);
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/cart";
    }

    @PostMapping("/decrease")
    public String decrease(@RequestParam Integer productId, HttpSession session) {
        Map<Integer, Integer> cart = getCartMap(session);
        cart.computeIfPresent(productId, (id, quantity) -> quantity > 1 ? quantity - 1 : null);
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Integer productId, HttpSession session) {
        Map<Integer, Integer> cart = getCartMap(session);
        cart.remove(productId);
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String paymentMethod,
                           @RequestParam Integer deliveryId,
                           @RequestParam(required = false) Integer accountAddressId,
                           Authentication authentication,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Account currentUser = currentAccountService.getCurrentAccount(authentication).orElse(null);

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("checkoutError", "Bạn cần đăng nhập trước khi đặt hàng.");
            return "redirect:/login";
        }

        if (getCartMap(session).isEmpty()) {
            return "redirect:/cart?error=empty_cart";
        }

        if (accountAddressId == null) {
            redirectAttributes.addFlashAttribute("checkoutError", "Vui lòng chọn địa chỉ giao hàng trước khi đặt hàng.");
            return "redirect:/cart?error=missing_address";
        }

        redirectAttributes.addFlashAttribute("selectedAccountAddressId", accountAddressId);

        try {
            if ("COD".equalsIgnoreCase(paymentMethod)) {
                OrderEntity order = checkoutService.createCodOrder(currentUser.getAccountId(), deliveryId, accountAddressId, session);
                return "redirect:/orders/invoice/" + order.getOrderId() + "?created=true";
            }

            if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) {
                Integer orderId = checkoutService
                        .createBankTransferOrder(currentUser.getAccountId(), deliveryId, accountAddressId, session)
                        .getOrderId();

                return "redirect:/payment/momo/create?orderId=" + orderId;
            }

            return "redirect:/cart?error=invalid_payment";
        } catch (IllegalArgumentException exception) {
            log.warn("Checkout validation failed. accountId={}, deliveryId={}, addressId={}, paymentMethod={}, reason={}",
                    currentUser.getAccountId(), deliveryId, accountAddressId, paymentMethod, exception.getMessage());
            return switch (exception.getMessage()) {
                case "empty_cart" -> "redirect:/cart?error=empty_cart";
                case "invalid_delivery" -> "redirect:/cart?error=invalid_delivery";
                case "missing_momo_payment", "missing_cod_payment" -> "redirect:/cart?error=payment_unavailable";
                case "invalid_product" -> "redirect:/cart?error=invalid_product";
                case "missing_checkout_address" -> {
                    redirectAttributes.addFlashAttribute("checkoutError", "Vui lòng chọn địa chỉ giao hàng trước khi đặt hàng.");
                    yield "redirect:/cart?error=missing_address";
                }
                case "invalid_checkout_address", "invalid_default_address" -> {
                    redirectAttributes.addFlashAttribute("checkoutError", "Địa chỉ giao hàng không hợp lệ hoặc không thuộc tài khoản này. Vui lòng chọn địa chỉ khác.");
                    yield "redirect:/cart?error=invalid_address";
                }
                default -> {
                    redirectAttributes.addFlashAttribute("checkoutError", "Không thể tạo đơn hàng lúc này. Vui lòng kiểm tra lại thông tin và thử lại.");
                    yield "redirect:/cart?error=checkout_failed";
                }
            };
        } catch (Exception exception) {
            log.error("Checkout failed. accountId={}, deliveryId={}, addressId={}, paymentMethod={}",
                    currentUser.getAccountId(), deliveryId, accountAddressId, paymentMethod, exception);
            redirectAttributes.addFlashAttribute("checkoutError", "Không thể tạo đơn hàng lúc này. Vui lòng kiểm tra lại thông tin và thử lại.");
            return "redirect:/cart?error=checkout_failed";
        }
    }

    private Map<Integer, Integer> getCartMap(HttpSession session) {
        Object cart = session.getAttribute(CART_SESSION_KEY);
        Map<Integer, Integer> normalized = new LinkedHashMap<>();

        if (cart instanceof Map<?, ?> existing) {
            existing.forEach((key, value) -> {
                Integer productId = toPositiveInteger(key);
                Integer quantity = toPositiveInteger(value);
                if (productId != null && quantity != null) {
                    normalized.put(productId, quantity);
                }
            });
        }

        session.setAttribute(CART_SESSION_KEY, normalized);
        return normalized;
    }

    private Integer toPositiveInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Integer converted = null;
            if (value instanceof Integer integerValue) {
                converted = integerValue;
            } else if (value instanceof Number numberValue) {
                converted = numberValue.intValue();
            } else if (value instanceof String stringValue && !stringValue.isBlank()) {
                converted = Integer.valueOf(stringValue.trim());
            }
            return converted != null && converted > 0 ? converted : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
