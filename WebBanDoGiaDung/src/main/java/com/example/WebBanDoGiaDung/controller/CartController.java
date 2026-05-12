package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.CartDto;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.Delivery;
import com.example.WebBanDoGiaDung.security.AccountPrincipal;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.CartService;
import com.example.WebBanDoGiaDung.service.CheckoutService;
import com.example.WebBanDoGiaDung.service.DeliveryService;
import com.example.WebBanDoGiaDung.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cart")
public class CartController {

    public static final String CART_SESSION_KEY = "CART";
    private final ProductService productService;
    private final DeliveryService deliveryService;
    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final AccountService accountService;

    public CartController(ProductService productService,
                          DeliveryService deliveryService,
                          CheckoutService checkoutService,
                          CartService cartService,
                          AccountService accountService) {
        this.productService = productService;
        this.deliveryService = deliveryService;
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.accountService = accountService;
    }

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        return cartService.getCartQuantity(session);
    }

    @GetMapping
    public String index(HttpSession session, Model model, Authentication authentication) {
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
        Account currentUser = resolveCurrentUser(authentication);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
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
                           Authentication authentication,
                           HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        if (getCartMap(session).isEmpty()) {
            return "redirect:/cart?error=empty_cart";
        }
        if (!(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            return "redirect:/login";
        }

        try {
            if ("COD".equalsIgnoreCase(paymentMethod)) {
                checkoutService.createCodOrder(principal.getAccount().getAccountId(), deliveryId, session);
                return "redirect:/profile#order-history";
            }
            if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) {
                Integer orderId = checkoutService.createBankTransferOrder(principal.getAccount().getAccountId(), deliveryId, session).getOrderId();
                return "redirect:/payment/momo/create?orderId=" + orderId;
            }
            return "redirect:/cart?error=invalid_payment";
        } catch (IllegalArgumentException exception) {
            return switch (exception.getMessage()) {
                case "empty_cart" -> "redirect:/cart?error=empty_cart";
                case "invalid_delivery" -> "redirect:/cart?error=invalid_delivery";
                case "missing_momo_payment", "missing_cod_payment" -> "redirect:/cart?error=payment_unavailable";
                case "invalid_product" -> "redirect:/cart?error=invalid_product";
                default -> "redirect:/profile?error=missing_default_address";
            };
        }
    }

    private Account resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            return null;
        }
        Integer accountId = principal.getAccount() != null ? principal.getAccount().getAccountId() : null;
        if (accountId != null) {
            return accountService.findById(accountId).orElse(null);
        }
        String email = principal.getUsername();
        if (email != null && !email.isBlank()) {
            return accountService.findByEmail(email.trim()).orElse(null);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getCartMap(HttpSession session) {
        Object cart = session.getAttribute(CART_SESSION_KEY);
        if (cart instanceof Map<?, ?> existing) {
            Map<Integer, Integer> normalized = new LinkedHashMap<>();
            existing.forEach((key, value) -> {
                if (key instanceof Integer productId && value instanceof Integer quantity) {
                    normalized.put(productId, quantity);
                }
            });
            session.setAttribute(CART_SESSION_KEY, normalized);
            return normalized;
        }
        Map<Integer, Integer> created = new LinkedHashMap<>();
        session.setAttribute(CART_SESSION_KEY, created);
        return created;
    }
}
