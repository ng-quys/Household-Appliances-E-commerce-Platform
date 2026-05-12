package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.entity.Brand;
import com.example.WebBanDoGiaDung.entity.Genre;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.service.BrandService;
import com.example.WebBanDoGiaDung.service.GenreService;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.service.ProductService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final BrandService brandService;
    private final GenreService genreService;
    private final OderDetailService oderDetailService;

    public AdminProductController(ProductService productService,
                                  BrandService brandService,
                                  GenreService genreService,
                                  OderDetailService oderDetailService) {
        this.productService = productService;
        this.brandService = brandService;
        this.genreService = genreService;
        this.oderDetailService = oderDetailService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String search,
                        @RequestParam(required = false, defaultValue = "default") String sortOrder,
                        Model model) {
        List<Product> products = productService.findAll().stream()
                .filter(x -> search == null || search.isBlank() || (x.getProductName() != null && x.getProductName().toLowerCase().contains(search.toLowerCase())))
                .sorted(switch (sortOrder) {
                    case "priceAsc" -> Comparator.comparing(x -> x.getPrice() == null ? 0D : x.getPrice());
                    case "priceDesc" -> Comparator.comparing((Product x) -> x.getPrice() == null ? 0D : x.getPrice()).reversed();
                    case "stockAsc" -> Comparator.comparingLong(this::resolveQuantity);
                    case "latest" -> Comparator.comparing(Product::getCreateAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed();
                    default -> Comparator.comparing(Product::getProductId, Comparator.nullsLast(Integer::compareTo)).reversed();
                })
                .toList();
        model.addAttribute("products", products);
        model.addAttribute("search", search);
        model.addAttribute("sortOrder", sortOrder);
        return "admin/product/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        if (!model.containsAttribute("product")) {
            model.addAttribute("product", new Product());
        }
        populateProductFormOptions(model);
        return "admin/product/create";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute("product") Product product, Model model, RedirectAttributes redirectAttributes) {
        String validationError = validateAndAttachRelations(product);
        if (validationError != null) {
            model.addAttribute("error", validationError);
            populateProductFormOptions(model);
            return "admin/product/create";
        }
        product.setCreateAt(LocalDateTime.now());
        product.setUpdateAt(LocalDateTime.now());
        if (product.getCreateBy() == null || product.getCreateBy().isBlank()) product.setCreateBy("admin");
        if (product.getUpdateBy() == null || product.getUpdateBy().isBlank()) product.setUpdateBy("admin");
        if (product.getView() == null) product.setView(0L);
        if (product.getBuyturn() == null) product.setBuyturn(0L);
        productService.save(product);
        redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công!");
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        if (!model.containsAttribute("product")) {
            Product product = productService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
            model.addAttribute("product", product);
        }
        populateProductFormOptions(model);
        return "admin/product/edit";
    }

    @PostMapping("/edit")
    public String editPost(@ModelAttribute("product") Product model, Model uiModel, RedirectAttributes redirectAttributes) {
        Product product = productService.findById(model.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + model.getProductId()));

        product.setProductName(model.getProductName());
        product.setImage(model.getImage());
        product.setPrice(model.getPrice());
        product.setBrandId(model.getBrandId());
        product.setGenreId(model.getGenreId());
        product.setQuantity(model.getQuantity());
        product.setType(model.getType());
        product.setStatus(model.getStatus());
        product.setDescription(model.getDescription());

        String validationError = validateAndAttachRelations(product);
        if (validationError != null) {
            uiModel.addAttribute("error", validationError);
            uiModel.addAttribute("product", product);
            populateProductFormOptions(uiModel);
            return "admin/product/edit";
        }

        product.setUpdateAt(LocalDateTime.now());
        if (product.getUpdateBy() == null || product.getUpdateBy().isBlank()) product.setUpdateBy("admin");
        if (product.getCreateBy() == null || product.getCreateBy().isBlank()) product.setCreateBy("admin");
        if (product.getCreateAt() == null) product.setCreateAt(LocalDateTime.now());
        if (product.getView() == null) product.setView(0L);
        if (product.getBuyturn() == null) product.setBuyturn(0L);
        productService.save(product);
        redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công!");
        return "redirect:/admin/products";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Product product = productService.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm để ẩn.");
            return "redirect:/admin/products";
        }

        boolean hasOrderDetails = !oderDetailService.findByProductId(id).isEmpty();
        product.setStatus("0");
        product.setUpdateAt(LocalDateTime.now());
        if (product.getUpdateBy() == null || product.getUpdateBy().isBlank()) {
            product.setUpdateBy("admin");
        }
        if (product.getBrand() == null && product.getBrandId() != null) {
            brandService.findById(product.getBrandId()).ifPresent(product::setBrand);
        }
        if (product.getGenre() == null && product.getGenreId() != null) {
            genreService.findById(product.getGenreId()).ifPresent(product::setGenre);
        }
        productService.save(product);

        if (hasOrderDetails) {
            redirectAttributes.addFlashAttribute("success", "Sản phẩm đã có trong đơn hàng, đã chuyển sang trạng thái ẩn.");
        } else {
            redirectAttributes.addFlashAttribute("success", "Đã ẩn sản phẩm thành công.");
        }
        return "redirect:/admin/products";
    }

    private void populateProductFormOptions(Model model) {
        model.addAttribute("brands", brandService.findAll());
        model.addAttribute("genres", genreService.findAll());
    }

    private long resolveQuantity(Product product) {
        try {
            return product != null && product.getQuantity() != null ? Long.parseLong(product.getQuantity().trim()) : 0L;
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private String validateAndAttachRelations(Product product) {
        if (product == null) {
            return "Dữ liệu sản phẩm không hợp lệ.";
        }
        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            return "Tên sản phẩm không được để trống.";
        }
        if (product.getPrice() == null || product.getPrice() <= 0) {
            return "Giá sản phẩm phải lớn hơn 0.";
        }
        if (product.getQuantity() == null || product.getQuantity().trim().isEmpty()) {
            return "Số lượng không được để trống.";
        }
        try {
            long quantity = Long.parseLong(product.getQuantity().trim());
            if (quantity < 0) {
                return "Số lượng không được nhỏ hơn 0.";
            }
        } catch (NumberFormatException exception) {
            return "Số lượng phải là số hợp lệ.";
        }
        if (product.getType() == null) {
            return "Loại sản phẩm là bắt buộc.";
        }
        if (product.getBrandId() == null) {
            return "Vui lòng chọn hãng sản xuất.";
        }
        if (product.getGenreId() == null) {
            return "Vui lòng chọn thể loại.";
        }

        Brand brand = brandService.findById(product.getBrandId()).orElse(null);
        if (brand == null) {
            return "Hãng sản xuất không hợp lệ.";
        }
        Genre genre = genreService.findById(product.getGenreId()).orElse(null);
        if (genre == null) {
            return "Thể loại không hợp lệ.";
        }

        product.setBrand(brand);
        product.setGenre(genre);
        product.setBrandId(brand.getBrandId());
        product.setGenreId(genre.getGenreId());
        return null;
    }
}
