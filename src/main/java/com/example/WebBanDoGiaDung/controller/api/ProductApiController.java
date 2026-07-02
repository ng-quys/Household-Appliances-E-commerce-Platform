package com.example.WebBanDoGiaDung.controller.api;

import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.service.ProductService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/productapi")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Object getProducts(@RequestParam(required = false) Integer brand,
                              @RequestParam(required = false) Integer genre,
                              @RequestParam(required = false) Integer min,
                              @RequestParam(required = false) Integer max,
                              @RequestParam(required = false) String search,
                              @RequestParam(defaultValue = "default") String sort,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "12") int pageSize) {

        Stream<Product> stream = productService.findByStatus("1").stream();
        if (brand != null) stream = stream.filter(p -> brand.equals(p.getBrandId()));
        if (genre != null) stream = stream.filter(p -> genre.equals(p.getGenreId()));
        if (min != null) stream = stream.filter(p -> p.getPrice() != null && p.getPrice() >= min);
        if (max != null) stream = stream.filter(p -> p.getPrice() != null && p.getPrice() <= max);
        if (search != null && !search.isBlank()) {
            String key = search.toLowerCase();
            stream = stream.filter(p -> p.getProductName() != null && p.getProductName().toLowerCase().contains(key));
        }

        Comparator<Product> comparator = Comparator.comparing(Product::getProductId, Comparator.nullsLast(Integer::compareTo));
        switch (sort) {
            case "az" -> comparator = Comparator.comparing(p -> p.getProductName() == null ? "" : p.getProductName());
            case "za" -> comparator = Comparator.comparing((Product p) -> p.getProductName() == null ? "" : p.getProductName()).reversed();
            case "price_asc" -> comparator = Comparator.comparing(p -> p.getPrice() == null ? 0D : p.getPrice());
            case "price_desc" -> comparator = Comparator.comparing((Product p) -> p.getPrice() == null ? 0D : p.getPrice()).reversed();
            case "newest" -> comparator = Comparator.comparing((Product p) -> p.getCreateAt() == null ? java.time.LocalDateTime.MIN : p.getCreateAt()).reversed();
            case "oldest" -> comparator = Comparator.comparing(p -> p.getCreateAt() == null ? java.time.LocalDateTime.MIN : p.getCreateAt());
            default -> { }
        }

        List<Product> filtered = stream.sorted(comparator).toList();
        int totalItems = filtered.size();
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(totalItems, from + pageSize);
        List<Map<String, Object>> data = filtered.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("product_id", p.getProductId());
                    map.put("product_name", p.getProductName());
                    map.put("price", p.getPrice());
                    map.put("image", p.getImage());
                    return map;
                })
                .collect(Collectors.toList());

        return Map.of(
                "totalItems", totalItems,
                "page", page,
                "pageSize", pageSize,
                "totalPage", (int) Math.ceil((double) totalItems / pageSize),
                "data", data
        );
    }

    @GetMapping("/{id}")
    public Object getProduct(@PathVariable Integer id) {
        Product p = productService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return Map.of(
                "product_id", p.getProductId(),
                "product_name", p.getProductName(),
                "price", p.getPrice(),
                "image", p.getImage(),
                "description", p.getDescription(),
                "brand_name", p.getBrand() != null ? p.getBrand().getBrandName() : null,
                "genre_name", p.getGenre() != null ? p.getGenre().getGenreName() : null,
                "genre_id", p.getGenreId(),
                "create_at", p.getCreateAt()
        );
    }

    @GetMapping("/related/{id}")
    public Object getRelatedProducts(@PathVariable Integer id) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return productService.findAll().stream()
                .filter(x -> x.getGenreId() != null && x.getGenreId().equals(product.getGenreId()))
                .filter(x -> !x.getProductId().equals(id))
                .limit(6)
                .map(x -> Map.of(
                        "product_id", x.getProductId(),
                        "product_name", x.getProductName(),
                        "price", x.getPrice(),
                        "image", x.getImage()))
                .toList();
    }
}
