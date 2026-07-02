package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.entity.Brand;
import com.example.WebBanDoGiaDung.repository.ProductRepository;
import com.example.WebBanDoGiaDung.service.BrandService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
@RequestMapping("/admin/brands")
public class AdminBrandController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BrandService brandService;
    private final ProductRepository productRepository;

    public AdminBrandController(BrandService brandService, ProductRepository productRepository) {
        this.brandService = brandService;
        this.productRepository = productRepository;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String search,
                        @RequestParam(required = false, defaultValue = "default") String sortOrder,
                        Model model) {
        String normalizedSearch = search != null ? search.trim().toLowerCase(Locale.ROOT) : "";

        List<Brand> brands = brandService.findAll().stream()
                .filter(brand -> normalizedSearch.isBlank() || containsBrandName(brand, normalizedSearch))
                .sorted(resolveSortComparator(sortOrder))
                .toList();

        model.addAttribute("brands", brands);
        model.addAttribute("search", search);
        model.addAttribute("sortOrder", sortOrder);
        return "admin/brand/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        if (!model.containsAttribute("brand")) {
            model.addAttribute("brand", new Brand());
        }
        return "admin/brand/create";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute("brand") Brand brand,
                             RedirectAttributes redirectAttributes) {
        String validationError = validateBrand(brand, null);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            redirectAttributes.addFlashAttribute("brand", brand);
            return "redirect:/admin/brands/create";
        }

        LocalDateTime now = LocalDateTime.now();
        brand.setBrandName(brand.getBrandName().trim());
        brand.setImage(normalizeImage(brand.getImage()));
        brand.setCreateAt(now);
        brand.setUpdateAt(now);
        brand.setCreateBy("admin");
        brand.setUpdateBy("admin");
        brandService.save(brand);
        redirectAttributes.addFlashAttribute("success", "Thêm thương hiệu thành công.");
        return "redirect:/admin/brands";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Brand brand = brandService.findById(id).orElse(null);
        if (brand == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thương hiệu.");
            return "redirect:/admin/brands";
        }
        if (!model.containsAttribute("brand")) {
            model.addAttribute("brand", brand);
        }
        return "admin/brand/edit";
    }

    @PostMapping("/edit/{id}")
    public String editPost(@PathVariable Integer id,
                           @ModelAttribute("brand") Brand formBrand,
                           RedirectAttributes redirectAttributes) {
        Brand existingBrand = brandService.findById(id).orElse(null);
        if (existingBrand == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thương hiệu.");
            return "redirect:/admin/brands";
        }

        String validationError = validateBrand(formBrand, id);
        if (validationError != null) {
            formBrand.setBrandId(id);
            formBrand.setCreateAt(existingBrand.getCreateAt());
            formBrand.setCreateBy(existingBrand.getCreateBy());
            formBrand.setUpdateAt(existingBrand.getUpdateAt());
            formBrand.setUpdateBy(existingBrand.getUpdateBy());
            redirectAttributes.addFlashAttribute("error", validationError);
            redirectAttributes.addFlashAttribute("brand", formBrand);
            return "redirect:/admin/brands/edit/" + id;
        }

        existingBrand.setBrandName(formBrand.getBrandName().trim());
        existingBrand.setImage(normalizeImage(formBrand.getImage()));
        existingBrand.setUpdateAt(LocalDateTime.now());
        existingBrand.setUpdateBy("admin");
        brandService.save(existingBrand);
        redirectAttributes.addFlashAttribute("success", "Cập nhật thương hiệu thành công.");
        return "redirect:/admin/brands";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Brand brand = brandService.findById(id).orElse(null);
        if (brand == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thương hiệu.");
            return "redirect:/admin/brands";
        }

        long productCount = productRepository.countByBrandBrandId(id);
        if (productCount > 0) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa thương hiệu vì đang có sản phẩm sử dụng.");
            return "redirect:/admin/brands";
        }

        brandService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Xóa thương hiệu thành công.");
        return "redirect:/admin/brands";
    }

    public String formatDate(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMATTER) : "N/A";
    }

    public String resolveBrandImage(Brand brand) {
        return brand != null && brand.getImage() != null && !brand.getImage().isBlank() ? brand.getImage().trim() : null;
    }

    private Comparator<Brand> resolveSortComparator(String sortOrder) {
        return switch (sortOrder) {
            case "latest" -> Comparator.comparing((Brand brand) -> brand.getCreateAt() == null ? LocalDateTime.MIN : brand.getCreateAt()).reversed();
            case "updated" -> Comparator.comparing((Brand brand) -> brand.getUpdateAt() == null ? LocalDateTime.MIN : brand.getUpdateAt()).reversed();
            default -> Comparator.comparing(Brand::getBrandName, Comparator.nullsFirst(String::compareToIgnoreCase));
        };
    }

    private boolean containsBrandName(Brand brand, String search) {
        return brand != null
                && brand.getBrandName() != null
                && brand.getBrandName().toLowerCase(Locale.ROOT).contains(search);
    }

    private String validateBrand(Brand brand, Integer currentBrandId) {
        if (brand == null || brand.getBrandName() == null || brand.getBrandName().trim().isBlank()) {
            return "Tên thương hiệu không được để trống.";
        }

        String normalizedName = brand.getBrandName().trim();
        Brand duplicatedBrand = brandService.findByBrandName(normalizedName).orElse(null);
        if (duplicatedBrand != null && (currentBrandId == null || !duplicatedBrand.getBrandId().equals(currentBrandId))) {
            return "Tên thương hiệu đã tồn tại.";
        }

        return null;
    }

    private String normalizeImage(String image) {
        return image != null && !image.trim().isBlank() ? image.trim() : null;
    }
}
