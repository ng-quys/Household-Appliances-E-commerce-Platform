package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.entity.Discount;
import com.example.WebBanDoGiaDung.service.DiscountService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/discounts")
public class AdminDiscountController {

    private final DiscountService discountService;

    public AdminDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String search,
                        @RequestParam(required = false) String code,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false, defaultValue = "default") String sortOrder,
                        Model model) {
        LocalDateTime now = LocalDateTime.now();
        List<Discount> discounts = discountService.findAll().stream()
                .filter(x -> search == null || search.isBlank() || (x.getDiscountName() != null && x.getDiscountName().toLowerCase().contains(search.toLowerCase())))
                .filter(x -> code == null || code.isBlank() || (x.getDiscountCode() != null && x.getDiscountCode().toLowerCase().contains(code.toLowerCase())))
                .filter(x -> {
                    if (status == null || status.isBlank()) return true;
                    return switch (status) {
                        case "active" -> x.getDiscountStart() != null && x.getDiscountEnd() != null && !x.getDiscountStart().isAfter(now) && !x.getDiscountEnd().isBefore(now);
                        case "expired" -> x.getDiscountEnd() != null && x.getDiscountEnd().isBefore(now);
                        case "upcoming" -> x.getDiscountStart() != null && x.getDiscountStart().isAfter(now);
                        case "soldout" -> x.getQuantity() != null && x.getQuantity() <= 0;
                        default -> true;
                    };
                })
                .sorted(switch (sortOrder) {
                    case "name" -> Comparator.comparing(x -> x.getDiscountName() == null ? "" : x.getDiscountName());
                    case "price" -> Comparator.comparing(x -> x.getDiscountPrice() == null ? 0D : x.getDiscountPrice(), Comparator.reverseOrder());
                    case "quantity" -> Comparator.comparing(x -> x.getQuantity() == null ? 0 : x.getQuantity(), Comparator.reverseOrder());
                    default -> Comparator.comparing(Discount::getDisscountId, Comparator.nullsLast(Integer::compareTo)).reversed();
                })
                .toList();
        model.addAttribute("discounts", discounts);
        return "admin/discount/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("discount", new Discount());
        return "admin/discount/create";
    }

    @PostMapping("/create")
    public String createPost(Discount model, RedirectAttributes redirectAttributes) {
        if (model.getDiscountStart() != null && model.getDiscountEnd() != null && !model.getDiscountEnd().isAfter(model.getDiscountStart())) {
            redirectAttributes.addFlashAttribute("error", "Ngày kết thúc phải sau ngày bắt đầu!");
            return "redirect:/admin/discounts/create";
        }
        model.setCreateAt(LocalDateTime.now());
        model.setUpdateAt(LocalDateTime.now());
        if (model.getCreateBy() == null) model.setCreateBy("admin");
        if (model.getUpdateBy() == null) model.setUpdateBy("admin");
        discountService.save(model);
        redirectAttributes.addFlashAttribute("success", "Thêm chương trình giảm giá thành công!");
        return "redirect:/admin/discounts";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("discount", discountService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount not found: " + id)));
        return "admin/discount/edit";
    }

    @PostMapping("/edit")
    public String editPost(Discount model, RedirectAttributes redirectAttributes) {
        Discount discount = discountService.findById(model.getDisscountId())
                .orElseThrow(() -> new IllegalArgumentException("Discount not found: " + model.getDisscountId()));
        discount.setDiscountName(model.getDiscountName());
        discount.setDiscountStart(model.getDiscountStart());
        discount.setDiscountEnd(model.getDiscountEnd());
        discount.setDiscountPrice(model.getDiscountPrice());
        discount.setQuantity(model.getQuantity());
        discount.setDiscountCode(model.getDiscountCode());
        discount.setUpdateAt(LocalDateTime.now());
        if (discount.getUpdateBy() == null) discount.setUpdateBy("admin");
        discountService.save(discount);
        redirectAttributes.addFlashAttribute("success", "Cập nhật chương trình giảm giá thành công!");
        return "redirect:/admin/discounts";
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        discountService.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Xóa chương trình giảm giá thành công!"));
    }
}
