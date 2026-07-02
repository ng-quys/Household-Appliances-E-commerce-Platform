package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.dto.AiProductClassificationResponse;
import com.example.WebBanDoGiaDung.entity.Brand;
import com.example.WebBanDoGiaDung.entity.Genre;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.service.BrandService;
import com.example.WebBanDoGiaDung.service.ExcelExportService;
import com.example.WebBanDoGiaDung.service.FileStorageService;
import com.example.WebBanDoGiaDung.service.GenreService;
import com.example.WebBanDoGiaDung.service.OderDetailService;
import com.example.WebBanDoGiaDung.service.ProductService;
import com.example.WebBanDoGiaDung.service.ai.AiProductClassificationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final BrandService brandService;
    private final GenreService genreService;
    private final OderDetailService oderDetailService;
    private final FileStorageService fileStorageService;
    private final AiProductClassificationService aiProductClassificationService;
    private final ExcelExportService excelExportService;

    public AdminProductController(ProductService productService,
                                  BrandService brandService,
                                  GenreService genreService,
                                  OderDetailService oderDetailService,
                                  FileStorageService fileStorageService,
                                  AiProductClassificationService aiProductClassificationService,
                                  ExcelExportService excelExportService) {
        this.productService = productService;
        this.brandService = brandService;
        this.genreService = genreService;
        this.oderDetailService = oderDetailService;
        this.fileStorageService = fileStorageService;
        this.aiProductClassificationService = aiProductClassificationService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam(required = false) String search,
                                                @RequestParam(required = false, defaultValue = "all") String statusFilter,
                                                @RequestParam(required = false, defaultValue = "default") String sortOrder) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, resolveSort(sortOrder));
        Page<Product> productPage = productService.findAdminProducts(search, statusFilter, pageable);
        List<Product> products = productPage.getContent();

        java.io.ByteArrayInputStream in = excelExportService.exportProducts(products);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=products_report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }

    @GetMapping
    public String index(@RequestParam(required = false) String search,
                        @RequestParam(required = false, defaultValue = "all") String statusFilter,
                        @RequestParam(required = false, defaultValue = "default") String sortOrder,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), resolvePageSize(size), resolveSort(sortOrder));
        Page<Product> productPage = productService.findAdminProducts(search, statusFilter, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("size", pageable.getPageSize());
        return "admin/product/index";
    }


    @PostMapping(value = "/ai-classify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<AiProductClassificationResponse> aiClassify(@RequestParam("imageFile") MultipartFile imageFile) {
        AiProductClassificationResponse response = aiProductClassificationService.classify(
                imageFile,
                genreService.findAll(),
                brandService.findAll()
        );
        return ResponseEntity.ok(response);
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
    public String createPost(@ModelAttribute("product") Product product,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            applyImageSource(product, imageFile, false, null);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("error", exception.getMessage());
            populateProductFormOptions(model);
            return "admin/product/create";
        }

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
    public String editPost(@ModelAttribute("product") Product model,
                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                           Model uiModel,
                           RedirectAttributes redirectAttributes) {
        Product product = productService.findById(model.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + model.getProductId()));

        product.setProductName(model.getProductName());
        product.setPrice(model.getPrice());
        product.setBrandId(model.getBrandId());
        product.setGenreId(model.getGenreId());
        product.setQuantity(model.getQuantity());
        product.setStatus(model.getStatus());
        product.setDescription(model.getDescription());

        try {
            applyImageSource(product, imageFile, true, model.getImage());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            uiModel.addAttribute("error", exception.getMessage());
            uiModel.addAttribute("product", product);
            populateProductFormOptions(uiModel);
            return "admin/product/edit";
        }

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

    private Sort resolveSort(String sortOrder) {
        return switch (sortOrder) {
            case "priceAsc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("productId"));
            case "priceDesc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("productId"));
            case "stockAsc" -> Sort.by(Sort.Order.asc("quantity"), Sort.Order.desc("productId"));
            case "stockDesc" -> Sort.by(Sort.Order.desc("quantity"), Sort.Order.desc("productId"));
            case "latest" -> Sort.by(Sort.Order.desc("createAt"), Sort.Order.desc("productId"));
            default -> Sort.by(Sort.Order.desc("productId"));
        };
    }

    private int resolvePageSize(int size) {
        return size <= 0 ? 10 : Math.min(size, 100);
    }

    private void applyImageSource(Product product, MultipartFile imageFile, boolean preserveExistingWhenBlank, String imageValue) {
        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImage(fileStorageService.storeProductImage(imageFile));
            return;
        }

        String normalizedImage = normalizeImage(imageValue != null ? imageValue : product.getImage());
        if (normalizedImage != null) {
            product.setImage(normalizedImage);
            return;
        }

        if (!preserveExistingWhenBlank) {
            product.setImage(null);
        }
    }

    private String normalizeImage(String image) {
        return image != null && !image.isBlank() ? image.trim() : null;
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
