package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.entity.Genre;
import com.example.WebBanDoGiaDung.security.CurrentAccountService;
import com.example.WebBanDoGiaDung.service.GenreService;
import com.example.WebBanDoGiaDung.service.ProductService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/products")
public class ProductController {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DETAIL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductService productService;
    private final GenreService genreService;
    private final CurrentAccountService currentAccountService;

    public ProductController(ProductService productService,
                             GenreService genreService,
                             CurrentAccountService currentAccountService) {
        this.productService = productService;
        this.genreService = genreService;
        this.currentAccountService = currentAccountService;
    }

    @ModelAttribute("genres")
    public List<Genre> getAllGenres() {
        return genreService.findAll();
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
                        Model model,
                        Authentication authentication) {

        Pageable pageable = createPageable(page, size, sort);

        Page<ProductCacheDto> productPage = productService.searchProducts(
                keyword,
                genreId,
                brandId,
                minPrice,
                maxPrice,
                pageable
        );

        addProductListAttributes(
                model,
                productPage,
                keyword,
                genreId,
                brandId,
                minPrice,
                maxPrice,
                sort
        );

        addCurrentUser(model, authentication);

        return "product/index";
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
                         Model model,
                         Authentication authentication) {

        Pageable pageable = createPageable(page, size, sort);

        Page<ProductCacheDto> productPage = productService.searchProducts(
                keyword,
                genreId,
                brandId,
                minPrice,
                maxPrice,
                pageable
        );

        addProductListAttributes(
                model,
                productPage,
                keyword,
                genreId,
                brandId,
                minPrice,
                maxPrice,
                sort
        );

        addCurrentUser(model, authentication);

        return "product/search";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Integer id,
                          Model model,
                          Authentication authentication) {
        ProductCacheDto product = productService.findProductDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        List<ProductCacheDto> relatedProducts = productService.findRelatedProducts(id, product.getGenreId(), 4);

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        model.addAttribute("formattedCreatedAt", "Chưa cập nhật");
        model.addAttribute("formattedUpdatedAt", "Chưa cập nhật");

        productService.findById(id).ifPresent(fullProduct -> {
            model.addAttribute("brandName", fullProduct.getBrand() != null ? fullProduct.getBrand().getBrandName() : "Đang cập nhật");
            model.addAttribute("genreName", fullProduct.getGenre() != null ? fullProduct.getGenre().getGenreName() : (product.getGenreName() != null ? product.getGenreName() : "Đang cập nhật"));
            model.addAttribute("formattedCreatedAt", fullProduct.getCreateAt() != null ? fullProduct.getCreateAt().format(DETAIL_DATE_FORMATTER) : "Chưa cập nhật");
            model.addAttribute("formattedUpdatedAt", fullProduct.getUpdateAt() != null ? fullProduct.getUpdateAt().format(DETAIL_DATE_FORMATTER) : "Chưa cập nhật");
        });

        if (!model.containsAttribute("brandName")) {
            model.addAttribute("brandName", "Đang cập nhật");
        }
        if (!model.containsAttribute("genreName")) {
            model.addAttribute("genreName", product.getGenreName() != null ? product.getGenreName() : "Đang cập nhật");
        }

        addCurrentUser(model, authentication);

        return "product/details";
    }

    @GetMapping("/ajax")
    public String ajax() {
        return "product/ajax";
    }

    private Pageable createPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Sort sortOption = Sort.unsorted();

        if ("priceAsc".equalsIgnoreCase(sort)) {
            sortOption = Sort.by("price").ascending();
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            sortOption = Sort.by("price").descending();
        } else if ("newest".equalsIgnoreCase(sort)) {
            sortOption = Sort.by("createAt").descending();
        }

        return PageRequest.of(safePage, safeSize, sortOption);
    }

    private void addProductListAttributes(Model model,
                                          Page<ProductCacheDto> productPage,
                                          String keyword,
                                          Integer genreId,
                                          Integer brandId,
                                          Double minPrice,
                                          Double maxPrice,
                                          String sort) {
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("genreId", genreId);
        model.addAttribute("brandId", brandId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
    }

    private void addCurrentUser(Model model, Authentication authentication) {
        currentAccountService.getCurrentAccount(authentication)
                .ifPresent(currentUser -> model.addAttribute("currentUser", currentUser));
    }
}