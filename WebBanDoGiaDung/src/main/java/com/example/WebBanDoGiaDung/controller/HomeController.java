package com.example.WebBanDoGiaDung.controller;

import com.example.WebBanDoGiaDung.dto.ChatRequest;
import com.example.WebBanDoGiaDung.dto.ChatResponse;
import com.example.WebBanDoGiaDung.dto.GenreCacheDto;
import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.repository.ProductRepository;
import com.example.WebBanDoGiaDung.security.AccountPrincipal;
import com.example.WebBanDoGiaDung.service.AccountService;
import com.example.WebBanDoGiaDung.service.GenreService;
import com.example.WebBanDoGiaDung.service.ProductService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/")
public class HomeController {

    private final GenreService genreService;
    private final ProductService productService;
    private final AccountService accountService;

    public HomeController(GenreService genreService, ProductService productService, AccountService accountService) {
        this.genreService = genreService;
        this.productService = productService;
        this.accountService = accountService;
    }

    @GetMapping({"", "/home", "/home/index"})
    public String index(Model model, Authentication authentication) {
        List<ProductCacheDto> featuredProducts = productService.findFeaturedProducts(8);

        List<GenreCacheDto> genres = featuredProducts.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(
                        ProductCacheDto::getId,
                        product -> new GenreCacheDto(
                                product.getId(),
                                product.getGenreName()
                        ),
                        (oldValue, newValue) -> oldValue,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(12)
                .toList();

        if (genres.isEmpty()) {
            genres = genreService.findAllGenreSummaries().stream()
                    .sorted(Comparator.comparing(g -> g.getGenreName() == null ? "" : g.getGenreName()))
                    .limit(12)
                    .toList();
        }
        List<ProductCacheDto> latestProducts = productService.findActiveProductCards().stream()
                .limit(12)
                .toList();

        model.addAttribute("genres", genres);
        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("latestProducts", latestProducts);
        model.addAttribute("placeholderImage", "https://via.placeholder.com/320x240?text=Do+gia+dung");

        Account currentUser = resolveCurrentUser(authentication);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        return "home/index";
    }

    @GetMapping("/home/about")
    public String about() {
        return "home/about";
    }

    @GetMapping("/home/chat")
    public String chat() {
        return "home/chat";
    }

    @PostMapping("/home/send-message")
    @ResponseBody
    public Object sendMessageToGemini(@RequestParam String userMessage) {
        return java.util.Map.of(
                "success", false,
                "reply", "Chưa nối Gemini trong khung controller này. Bước sau có thể tách riêng AI service để gọi API an toàn hơn.",
                "userMessage", userMessage
        );
    }

    @GetMapping("/home/banner")
    public String banner() {
        return "home/banner";
    }

    @GetMapping("/home/benefits")
    public String benefits() {
        return "home/benefits";
    }

    @GetMapping("/home/new-products")
    public String newProducts(Model model) {
        model.addAttribute("products", productService.findActiveProductCards().stream()
                .limit(10)
                .toList());
        return "home/fragments/new-products";
    }

    @GetMapping("/home/categories")
    public String categories() {
        return "home/fragments/categories";
    }

    private Account resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            return null;
        }
        Integer accountId = principal.getAccountId();
        if (accountId != null) {
            return accountService.findById(accountId).orElse(null);
        }
        String email = principal.getUsername();
        if (email != null && !email.isBlank()) {
            return accountService.findByEmail(email.trim()).orElse(null);
        }
        return null;
    }

    @GetMapping("/home/best-seller")
    public String bestSeller(Model model) {
        model.addAttribute("products", productService.findActiveProductCards().stream()
                .sorted(Comparator.comparing(p -> p.getBuyturn() == null ? 0L : p.getBuyturn(), Comparator.reverseOrder()))
                .limit(10)
                .toList());
        return "home/fragments/best-seller";
    }

    @RestController
    @RequestMapping("/api/chat")
    public class AiChatController {

        @Value("${gemini.api.key}")
        private String apiKey;

        private final ProductRepository productRepository;

        public AiChatController(ProductRepository productRepository) {
            this.productRepository = productRepository;
        }

        @PostMapping
        public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
            try {
                String modelName = "gemini-2.5-flash";

                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + modelName
                        + ":generateContent?key="
                        + apiKey;

                List<Product> products = productRepository.findByStatus("1");

                String productContext = products.stream()
                        .map(p -> p.getProductName() + " - Giá: " + String.format("%,.0f", p.getPrice()) + " đ")
                        .collect(Collectors.joining("; "));

                String systemPrompt = """
                    Bạn là nhân viên tư vấn của cửa hàng household.
                    Dưới đây là DANH SÁCH SẢN PHẨM VÀ GIÁ BÁN hiện tại của shop: [%s]

                    YÊU CẦU TRẢ LỜI:
                    1. Nếu khách hỏi về sản phẩm CÓ trong danh sách: Hãy báo giá chính xác như trong dữ liệu trên và mời khách mua.
                    2. Nếu khách hỏi giá: Tuyệt đối chỉ lấy giá từ danh sách trên, không được tự bịa giá.
                    3. Nếu khách hỏi sản phẩm KHÔNG có: Báo là shop chưa kinh doanh.
                    4. Trả lời ngắn gọn, lịch sự, dùng kính ngữ.

                    Câu hỏi của khách: "%s"
                    """.formatted(productContext, request.getMessage());

                RestTemplate restTemplate = new RestTemplate();

                Map<String, Object> body = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(
                                        Map.of("text", systemPrompt)
                                ))
                        )
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

                Map responseBody = response.getBody();

                if (responseBody == null) {
                    return ResponseEntity.ok(new ChatResponse(false, "AI không phản hồi."));
                }

                List candidates = (List) responseBody.get("candidates");

                if (candidates == null || candidates.isEmpty()) {
                    return ResponseEntity.ok(new ChatResponse(false, "AI không phản hồi."));
                }

                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                List parts = (List) content.get("parts");
                Map firstPart = (Map) parts.get(0);

                String aiReply = firstPart.get("text").toString();

                return ResponseEntity.ok(new ChatResponse(true, aiReply));

            } catch (Exception e) {
                return ResponseEntity.ok(new ChatResponse(false, "Lỗi hệ thống: " + e.getMessage()));
            }
        }
    }
}
