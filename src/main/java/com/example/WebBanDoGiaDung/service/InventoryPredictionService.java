package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.InventoryPredictionDto;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.repository.OderDetailRepository;
import com.example.WebBanDoGiaDung.repository.ProductRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryPredictionService {

    private final ProductRepository productRepository;
    private final OderDetailRepository oderDetailRepository;

    /**
     * Trả về toàn bộ sản phẩm đang bán kèm dự đoán tồn kho.
     * Dùng cho dashboard admin để tính thống kê nhanh và biểu đồ.
     */
    public List<InventoryPredictionDto> predictAllActiveProducts() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);

        Map<Integer, Long> soldMap = new HashMap<>();
        List<Object[]> soldData = oderDetailRepository.getSoldQuantityByProductFromDate(fromDate);

        for (Object[] row : soldData) {
            Integer productId = ((Number) row[0]).intValue();
            Long soldQuantity = row[1] == null ? 0L : ((Number) row[1]).longValue();
            soldMap.put(productId, soldQuantity);
        }

        return productRepository.findAll().stream()
                .filter(product -> "1".equals(product.getStatus()))
                .map(product -> buildPrediction(product, soldMap))
                .sorted(Comparator
                        .comparingInt((InventoryPredictionDto item) -> warningPriority(item.getWarningLevel()))
                        .thenComparing(InventoryPredictionDto::getSuggestedImportQuantity, Comparator.reverseOrder())
                        .thenComparing(InventoryPredictionDto::getCurrentStock))
                .toList();
    }

    /**
     * Trả về các sản phẩm cần admin chú ý nhập thêm.
     */
    public List<InventoryPredictionDto> predictProductsNeedImport() {
        return predictAllActiveProducts().stream()
                .filter(item -> item.getSuggestedImportQuantity() > 0
                        || item.getCurrentStock() <= 5
                        || "Cần nhập gấp".equals(item.getWarningLevel())
                        || "Sắp hết hàng".equals(item.getWarningLevel())
                        || "Có nguy cơ thiếu hàng".equals(item.getWarningLevel()))
                .limit(20)
                .toList();
    }

    private InventoryPredictionDto buildPrediction(Product product, Map<Integer, Long> soldMap) {
        Integer productId = product.getProductId();

        long currentStock = parseQuantity(product.getQuantity());
        long soldLast30Days = soldMap.getOrDefault(productId, 0L);

        // Nếu chưa có đơn hoàn thành trong 30 ngày, dùng buyturn làm dữ liệu tham khảo nhẹ
        // để dashboard không bị trống hoàn toàn khi dữ liệu đơn hàng còn ít.
        if (soldLast30Days == 0 && product.getBuyturn() != null && product.getBuyturn() > 0) {
            soldLast30Days = Math.max(1L, Math.round(product.getBuyturn() * 0.15));
        }

        long predictedDemandNext30Days = soldLast30Days;
        double averageDailySales = Math.round((soldLast30Days / 30.0) * 100.0) / 100.0;

        Long estimatedDaysRemaining = averageDailySales > 0
                ? Math.max(0L, (long) Math.floor(currentStock / averageDailySales))
                : null;

        long safetyStock = Math.max(2L, Math.round(predictedDemandNext30Days * 0.2));

        long suggestedImportQuantity = Math.max(
                0L,
                predictedDemandNext30Days + safetyStock - currentStock
        );

        String warningLevel = resolveWarningLevel(
                currentStock,
                predictedDemandNext30Days,
                suggestedImportQuantity,
                estimatedDaysRemaining
        );

        return new InventoryPredictionDto(
                productId,
                product.getProductName() != null ? product.getProductName() : "Sản phẩm",
                currentStock,
                soldLast30Days,
                predictedDemandNext30Days,
                suggestedImportQuantity,
                warningLevel,
                averageDailySales,
                estimatedDaysRemaining
        );
    }

    private String resolveWarningLevel(long currentStock,
                                       long predictedDemand,
                                       long suggestedImportQuantity,
                                       Long estimatedDaysRemaining) {
        if (currentStock <= 0) {
            return "Cần nhập gấp";
        }

        if (estimatedDaysRemaining != null && estimatedDaysRemaining <= 7) {
            return "Cần nhập gấp";
        }

        if (currentStock <= 5 || (estimatedDaysRemaining != null && estimatedDaysRemaining <= 15)) {
            return "Sắp hết hàng";
        }

        if (suggestedImportQuantity > 0) {
            return "Nên nhập thêm";
        }

        if (predictedDemand > 0 && currentStock <= predictedDemand) {
            return "Có nguy cơ thiếu hàng";
        }

        return "Ổn định";
    }

    private int warningPriority(String warningLevel) {
        if ("Cần nhập gấp".equals(warningLevel)) {
            return 1;
        }

        if ("Sắp hết hàng".equals(warningLevel)) {
            return 2;
        }

        if ("Nên nhập thêm".equals(warningLevel)) {
            return 3;
        }

        if ("Có nguy cơ thiếu hàng".equals(warningLevel)) {
            return 4;
        }

        return 5;
    }

    private long parseQuantity(String quantity) {
        try {
            return quantity != null && !quantity.trim().isEmpty()
                    ? Long.parseLong(quantity.trim())
                    : 0L;
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}
