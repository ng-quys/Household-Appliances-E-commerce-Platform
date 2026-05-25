package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.Genre;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.security.AccountPrincipal;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.GenreService;
import com.example.WebBanDoGiaDung.service.ProductService;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final AccountService accountService;
    private final GenreService genreService;

    public ProductController(ProductService productService,
                             AccountService accountService,
                             GenreService genreService) {
        this.productService = productService;
        this.accountService = accountService;
        this.genreService = genreService;
    }

    // Thêm method này vào class
    @ModelAttribute("genres")
    public List<Genre> getAllGenres() {
        return genreService.findAll();   // hoặc genreService.findAllActive()
    }

    @GetMapping
    public String index(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Integer genreId,
                        @RequestParam(required = false) Integer brandId,
                        @RequestParam(required = false) Double minPrice,
                        @RequestParam(required = false) Double maxPrice,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "12") int size,
                        @RequestParam(required = false) String sort,
                        Model model, Authentication authentication) {

        Pageable pageable = createPageable(page, size, sort);
        Page<ProductCacheDto> productPage = productService.searchProducts(
                keyword, genreId, brandId, minPrice, maxPrice, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("genreId", genreId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);

        addCurrentUser(model, authentication);
        return "product/index";
    }

    // Giữ lại details
    @GetMapping("/{id}")
    public String details(@PathVariable Integer id, Model model, Authentication authentication) {
        ProductCacheDto product = productService.findProductDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        model.addAttribute("product", product);
        addCurrentUser(model, authentication);
        return "product/details";
    }

    // Helper tạo Pageable + Sort
    private Pageable createPageable(int page, int size, String sort) {
        Sort sortOption = Sort.unsorted();
        if ("priceAsc".equalsIgnoreCase(sort)) {
            sortOption = Sort.by("price").ascending();
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            sortOption = Sort.by("price").descending();
        } else if ("newest".equalsIgnoreCase(sort)) {
            sortOption = Sort.by("createAt").descending();
        }
        return PageRequest.of(page, Math.min(size, 50), sortOption);
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword,
                         @RequestParam(required = false) Integer genreId,
                         @RequestParam(required = false) Integer brandId,
                         @RequestParam(required = false) Double minPrice,
                         @RequestParam(required = false) Double maxPrice,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "12") int size,
                         @RequestParam(required = false) String sort,
                         Model model, Authentication authentication) {

        Pageable pageable = createPageable(page, size, sort);
        Page<ProductCacheDto> productPage = productService.searchProducts(keyword, genreId, brandId, minPrice, maxPrice, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("genreId", genreId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        addCurrentUser(model, authentication);
        return "product/search";
    }




    @GetMapping("/ajax")
    public String ajax() {
        return "product/ajax";
    }



    private void addCurrentUser(Model model, Authentication authentication) {
        Account currentUser = resolveCurrentUser(authentication);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }
    }

    private Account resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        Integer accountId = null;
        String email = null;

        if (principal instanceof AccountPrincipal accountPrincipal) {
            accountId = accountPrincipal.getAccount() != null ? accountPrincipal.getAccount().getAccountId() : null;
            email = accountPrincipal.getUsername();
        } else if (principal instanceof AccountPrincipal accountPrincipal) {
            accountId = accountPrincipal.getAccount() != null ? accountPrincipal.getAccount().getAccountId() : null;
            email = accountPrincipal.getUsername();
        } else if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (authentication.getName() != null && !authentication.getName().isBlank()) {
            email = authentication.getName();
        }

        if (accountId != null) {
            return accountService.findById(accountId).orElse(null);
        }
        if (email != null && !email.isBlank()) {
            return accountService.findByEmail(email.trim()).orElse(null);
        }
        return null;
    }


}
