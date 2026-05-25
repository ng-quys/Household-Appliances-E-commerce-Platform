package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.dto.InventoryPredictionDto;
import com.example.WebBanDoGiaDung.repository.ProductRepository;
import com.example.WebBanDoGiaDung.service.InventoryPredictionService;
import com.example.WebBanDoGiaDung.service.RevenueForecastService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminInventoryPredictionController {

    private final InventoryPredictionService inventoryPredictionService;
    private final RevenueForecastService revenueForecastService;
    private final ProductRepository productRepository;

    public AdminInventoryPredictionController(InventoryPredictionService inventoryPredictionService,
                                              RevenueForecastService revenueForecastService,
                                              ProductRepository productRepository) {
        this.inventoryPredictionService = inventoryPredictionService;
        this.revenueForecastService = revenueForecastService;
        this.productRepository = productRepository;
    }

    @GetMapping("/inventory-prediction")
    public String inventoryPrediction(Model model) {
        List<InventoryPredictionDto> allPredictions = inventoryPredictionService.predictAllActiveProducts();
        List<InventoryPredictionDto> needImportPredictions = inventoryPredictionService.predictProductsNeedImport();

        long urgentCount = allPredictions.stream()
                .filter(item -> "Cần nhập gấp".equals(item.getWarningLevel()))
                .count();

        long lowStockCount = allPredictions.stream()
                .filter(item -> "Sắp hết hàng".equals(item.getWarningLevel())
                        || "Có nguy cơ thiếu hàng".equals(item.getWarningLevel()))
                .count();

        long needImportCount = allPredictions.stream()
                .filter(item -> item.getSuggestedImportQuantity() != null && item.getSuggestedImportQuantity() > 0)
                .count();

        long stableCount = allPredictions.stream()
                .filter(item -> "Ổn định".equals(item.getWarningLevel()))
                .count();

        long totalSuggestedImport = allPredictions.stream()
                .mapToLong(item -> item.getSuggestedImportQuantity() == null ? 0L : item.getSuggestedImportQuantity())
                .sum();

        long totalProducts = productRepository.count();
        long activeProducts = productRepository.findByStatus("1").size();
        long hiddenProducts = totalProducts - activeProducts;

        model.addAttribute("allInventoryPredictions", allPredictions);
        model.addAttribute("inventoryPredictions", needImportPredictions);
        model.addAttribute("forecastData", revenueForecastService.forecastNextMonths(6));
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("activeProducts", activeProducts);
        model.addAttribute("hiddenProducts", hiddenProducts);
        model.addAttribute("urgentCount", urgentCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("needImportCount", needImportCount);
        model.addAttribute("stableCount", stableCount);
        model.addAttribute("totalSuggestedImport", totalSuggestedImport);

        return "admin/inventory-prediction/index";
    }
}
