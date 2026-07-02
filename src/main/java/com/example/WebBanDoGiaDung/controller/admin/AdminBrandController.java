package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.entity.Brand;
import com.example.WebBanDoGiaDung.repository.ProductRepository;
import com.example.WebBanDoGiaDung.service.BrandService;
import com.example.WebBanDoGiaDung.service.FileStorageService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/brands")
public class AdminBrandController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BrandService brandService;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public AdminBrandController(BrandService brandService,
                                ProductRepository productRepository,
                                FileStorageService fileStorageService) {
        this.brandService = brandService;
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String search,
                        @RequestParam(required = false, defaultValue = "default") String sortOrder,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), resolvePageSize(size), resolveSort(sortOrder));
        Page<Brand> brandPage = brandService.findAdminBrands(search, pageable);

        model.addAttribute("brands", brandPage.getContent());
        model.addAttribute("brandPage", brandPage);
        model.addAttribute("search", search);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("size", pageable.getPageSize());
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
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            applyBrandImage(brand, imageFile, false, null);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "brand", bindingResult);
            model.addAttribute("brand", brand);
            return "admin/brand/create";
        }

        String validationError = validateBrand(brand, null);
        if (validationError != null) {
            model.addAttribute("error", validationError);
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "brand", bindingResult);
            model.addAttribute("brand", brand);
            return "admin/brand/create";
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
                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Brand existingBrand = brandService.findById(id).orElse(null);
        if (existingBrand == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thương hiệu.");
            return "redirect:/admin/brands";
        }

        try {
            applyBrandImage(formBrand, imageFile, true, formBrand.getImage());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            formBrand.setBrandId(id);
            formBrand.setCreateAt(existingBrand.getCreateAt());
            formBrand.setCreateBy(existingBrand.getCreateBy());
            formBrand.setUpdateAt(existingBrand.getUpdateAt());
            formBrand.setUpdateBy(existingBrand.getUpdateBy());
            model.addAttribute("error", exception.getMessage());
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "brand", bindingResult);
            model.addAttribute("brand", formBrand);
            return "admin/brand/edit";
        }

        String validationError = validateBrand(formBrand, id);
        if (validationError != null) {
            formBrand.setBrandId(id);
            formBrand.setCreateAt(existingBrand.getCreateAt());
            formBrand.setCreateBy(existingBrand.getCreateBy());
            formBrand.setUpdateAt(existingBrand.getUpdateAt());
            formBrand.setUpdateBy(existingBrand.getUpdateBy());
            model.addAttribute("error", validationError);
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "brand", bindingResult);
            model.addAttribute("brand", formBrand);
            return "admin/brand/edit";
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

    private Sort resolveSort(String sortOrder) {
        return switch (sortOrder) {
            case "latest" -> Sort.by(Sort.Order.desc("createAt"), Sort.Order.desc("brandId"));
            case "updated" -> Sort.by(Sort.Order.desc("updateAt"), Sort.Order.desc("brandId"));
            default -> Sort.by(Sort.Order.asc("brandName"), Sort.Order.asc("brandId"));
        };
    }

    private int resolvePageSize(int size) {
        return size <= 0 ? 10 : Math.min(size, 100);
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

    private void applyBrandImage(Brand brand, MultipartFile imageFile, boolean preserveExistingWhenBlank, String imageValue) {
        if (imageFile != null && !imageFile.isEmpty()) {
            brand.setImage(fileStorageService.storeBrandImage(imageFile));
            return;
        }

        String normalizedImage = normalizeImage(imageValue != null ? imageValue : brand.getImage());
        if (normalizedImage != null) {
            brand.setImage(normalizedImage);
            return;
        }

        if (!preserveExistingWhenBlank) {
            brand.setImage(null);
        }
    }

    private String normalizeImage(String image) {
        return image != null && !image.trim().isBlank() ? image.trim() : null;
    }
}
