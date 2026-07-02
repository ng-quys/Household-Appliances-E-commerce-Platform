package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.entity.Genre;
import com.example.WebBanDoGiaDung.repository.ProductRepository;
import com.example.WebBanDoGiaDung.service.GenreService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/genres")
public class AdminGenreController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final GenreService genreService;
    private final ProductRepository productRepository;

    public AdminGenreController(GenreService genreService, ProductRepository productRepository) {
        this.genreService = genreService;
        this.productRepository = productRepository;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String search,
                        @RequestParam(required = false, defaultValue = "default") String sortOrder,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), resolvePageSize(size), resolveSort(sortOrder));
        Page<Genre> genrePage = genreService.findAdminGenres(search, pageable);

        model.addAttribute("genres", genrePage.getContent());
        model.addAttribute("genrePage", genrePage);
        model.addAttribute("search", search);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("size", pageable.getPageSize());
        return "admin/genre/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        if (!model.containsAttribute("genre")) {
            model.addAttribute("genre", new Genre());
        }
        return "admin/genre/create";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute("genre") Genre genre,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        String validationError = validateGenre(genre, null);
        if (validationError != null) {
            model.addAttribute("error", validationError);
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "genre", bindingResult);
            model.addAttribute("genre", genre);
            return "admin/genre/create";
        }

        LocalDateTime now = LocalDateTime.now();
        genre.setGenreName(genre.getGenreName().trim());
        genre.setCreateAt(now);
        genre.setUpdateAt(now);
        genre.setCreateBy("admin");
        genre.setUpdateBy("admin");
        genreService.save(genre);
        redirectAttributes.addFlashAttribute("success", "Thêm danh mục thành công.");
        return "redirect:/admin/genres";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Genre genre = genreService.findById(id).orElse(null);
        if (genre == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục.");
            return "redirect:/admin/genres";
        }
        if (!model.containsAttribute("genre")) {
            model.addAttribute("genre", genre);
        }
        return "admin/genre/edit";
    }

    @PostMapping("/edit/{id}")
    public String editPost(@PathVariable Integer id,
                           @ModelAttribute("genre") Genre formGenre,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Genre existingGenre = genreService.findById(id).orElse(null);
        if (existingGenre == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục.");
            return "redirect:/admin/genres";
        }

        String validationError = validateGenre(formGenre, id);
        if (validationError != null) {
            formGenre.setGenreId(id);
            formGenre.setCreateAt(existingGenre.getCreateAt());
            formGenre.setCreateBy(existingGenre.getCreateBy());
            formGenre.setUpdateAt(existingGenre.getUpdateAt());
            formGenre.setUpdateBy(existingGenre.getUpdateBy());
            model.addAttribute("error", validationError);
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "genre", bindingResult);
            model.addAttribute("genre", formGenre);
            return "admin/genre/edit";
        }

        existingGenre.setGenreName(formGenre.getGenreName().trim());
        existingGenre.setUpdateAt(LocalDateTime.now());
        existingGenre.setUpdateBy("admin");
        genreService.save(existingGenre);
        redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục thành công.");
        return "redirect:/admin/genres";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Genre genre = genreService.findById(id).orElse(null);
        if (genre == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục.");
            return "redirect:/admin/genres";
        }

        long productCount = productRepository.countByGenreGenreId(id);
        if (productCount > 0) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa danh mục vì đang có sản phẩm sử dụng.");
            return "redirect:/admin/genres";
        }

        genreService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Xóa danh mục thành công.");
        return "redirect:/admin/genres";
    }

    public String formatDate(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMATTER) : "N/A";
    }

    private Sort resolveSort(String sortOrder) {
        return switch (sortOrder) {
            case "latest" -> Sort.by(Sort.Order.desc("createAt"), Sort.Order.desc("genreId"));
            case "updated" -> Sort.by(Sort.Order.desc("updateAt"), Sort.Order.desc("genreId"));
            default -> Sort.by(Sort.Order.asc("genreName"), Sort.Order.asc("genreId"));
        };
    }

    private int resolvePageSize(int size) {
        return size <= 0 ? 10 : Math.min(size, 100);
    }

    private String validateGenre(Genre genre, Integer currentGenreId) {
        if (genre == null || genre.getGenreName() == null || genre.getGenreName().trim().isBlank()) {
            return "Tên danh mục không được để trống.";
        }
        if (genreService.existsByGenreNameIgnoreCaseAndIdNot(genre.getGenreName(), currentGenreId)) {
            return "Tên danh mục đã tồn tại.";
        }
        return null;
    }
}
