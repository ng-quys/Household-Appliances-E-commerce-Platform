package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.security.AccountPrincipal;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.ProductService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final AccountService accountService;

    public ProductController(ProductService productService, AccountService accountService) {
        this.productService = productService;
        this.accountService = accountService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String sort, Model model, Authentication authentication) {
        model.addAttribute("products", productService.applyPriceSort(productService.findActiveProductCards(), sort));
        model.addAttribute("sort", sort);
        addCurrentUser(model, authentication);
        return "product/index";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Integer id, Model model, Authentication authentication) {
        ProductCacheDto product = productService.findProductDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        model.addAttribute("product", product);
        addCurrentUser(model, authentication);
        return "product/details";
    }

    @GetMapping("/ajax")
    public String ajax() {
        return "product/ajax";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String genre,
                         @RequestParam(required = false) String priceFrom,
                         @RequestParam(required = false) String priceTo,
                         @RequestParam(required = false) String sort,
                         Model model,
                         Authentication authentication) {
        List<ProductCacheDto> products = (keyword == null || keyword.isBlank())
                ? productService.findActiveProductCards()
                : productService.searchByName(keyword).stream()
                    .filter(p -> "1".equals(p.getStatus()))
                    .map(ProductCacheDto::fromEntity)
                    .toList();
        products = productService.applyPriceSort(products, sort);
        model.addAttribute("keyword", keyword);
        model.addAttribute("genre", genre);
        model.addAttribute("priceFrom", priceFrom);
        model.addAttribute("priceTo", priceTo);
        model.addAttribute("sort", sort);
        model.addAttribute("products", products);
        addCurrentUser(model, authentication);
        return "product/search";
    }

    private void addCurrentUser(Model model, Authentication authentication) {
        Account currentUser = resolveCurrentUser(authentication);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
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
}
