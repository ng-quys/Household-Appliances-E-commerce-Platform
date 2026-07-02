package com.example.WebBanDoGiaDung.controller.admin;

import com.example.WebBanDoGiaDung.dto.AdminDashboardDto;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.repository.AccountRepository;
import com.example.WebBanDoGiaDung.repository.BrandRepository;
import com.example.WebBanDoGiaDung.repository.OrderEntityRepository;
import com.example.WebBanDoGiaDung.repository.ProductRepository;
import com.example.WebBanDoGiaDung.service.RevenueForecastService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminHomeController {

    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final OrderEntityRepository orderEntityRepository;
    private final AccountRepository accountRepository;
    private final RevenueForecastService revenueForecastService;

    public AdminHomeController(ProductRepository productRepository,
                               BrandRepository brandRepository,
                               OrderEntityRepository orderEntityRepository,
                               AccountRepository accountRepository,
                               RevenueForecastService revenueForecastService) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.orderEntityRepository = orderEntityRepository;
        this.accountRepository = accountRepository;
        this.revenueForecastService = revenueForecastService;
    }

    @GetMapping({"", "/"})
    public String adminRoot() {
        return "redirect:/admin/home";
    }

    @GetMapping({"/home", "/home/"})
    public String index(@RequestParam(defaultValue = "7days") String range, Model model) {
        List<Product> products = productRepository.findAll();
        List<OrderEntity> orders = orderEntityRepository.findAll();
        List<Account> accounts = accountRepository.findAll();

        LocalDate today = LocalDate.now();

        String selectedRange = normalizeRange(range);
        LocalDate startDate = resolveStartDate(selectedRange, today);

        List<OrderEntity> filteredOrders = orders.stream()
                .filter(order -> isWithinRange(order.getOrderDate(), startDate, today, selectedRange))
                .sorted(Comparator.comparing(OrderEntity::getOrderDate).reversed())
                .toList();

        Map<LocalDate, DailyChartPoint> dailyPoints = buildDailyPoints(filteredOrders, startDate, today, selectedRange);

        long activeProducts = products.stream().filter(this::isActiveProduct).count();
        long hiddenProducts = products.size() - activeProducts;

        AdminDashboardDto dashboard = AdminDashboardDto.builder()
                .totalProducts(products.size())
                .activeProducts(activeProducts)
                .hiddenProducts(hiddenProducts)
                .totalBrands(brandRepository.count())
                .totalOrders(filteredOrders.size())
                .totalAccounts(accounts.size())
                .totalRevenue(filteredOrders.stream()
                        .filter(this::isRevenueOrder)
                        .mapToDouble(order -> order.getTotal() != null ? order.getTotal() : 0D)
                        .sum())
                .lowStockProducts(products.stream().filter(this::isLowStock).count())
                .selectedRange(range)
                .selectedRangeLabel(resolveRangeLabel(range))
                .chartLabels(dailyPoints.keySet().stream()
                        .map(date -> date.format(DAY_LABEL_FORMATTER))
                        .toList())
                .revenueSeries(dailyPoints.values().stream()
                        .map(DailyChartPoint::revenue)
                        .toList())
                .orderSeries(dailyPoints.values().stream()
                        .map(DailyChartPoint::orders)
                        .toList())
                .recentOrders(orderEntityRepository.findTop10ByOrderByOrderDateDescOrderIdDesc()
                        .stream()
                        .map(this::toRecentOrderItem)
                        .toList())
                .lowStockItems(products.stream()
                        .filter(this::isLowStock)
                        .sorted(Comparator.comparingLong(this::safeQuantity))
                        .limit(5)
                        .map(this::toLowStockItem)
                        .toList())
                .recentAccounts(accountRepository.findTop5ByOrderByCreateAtDescAccountIdDesc()
                        .stream()
                        .map(this::toRecentAccountItem)
                        .toList())
                .build();

        model.addAttribute("dashboard", dashboard);

        // Dữ liệu cho chức năng dự báo doanh thu theo tháng
        model.addAttribute("forecastData", revenueForecastService.forecastNextMonths(6));

        return "admin/home/index";
    }

    private String normalizeRange(String range) {
        if (range == null || range.isBlank()) {
            return "7days";
        }

        return switch (range) {
            case "today", "7days", "30days", "all" -> range;
            default -> "7days";
        };
    }

    private String resolveRangeLabel(String range) {
        return switch (range) {
            case "today" -> "Hôm nay";
            case "30days" -> "30 ngày gần nhất";
            case "all" -> "Toàn bộ dữ liệu";
            default -> "7 ngày gần nhất";
        };
    }

    private LocalDate resolveStartDate(String range, LocalDate today) {
        return switch (range) {
            case "today" -> today;
            case "30days" -> today.minusDays(29);
            case "all" -> null;
            default -> today.minusDays(6);
        };
    }

    private boolean isWithinRange(LocalDateTime dateTime, LocalDate startDate, LocalDate today, String range) {
        if (dateTime == null) {
            return false;
        }

        if ("all".equalsIgnoreCase(range)) {
            return true;
        }

        LocalDate date = dateTime.toLocalDate();

        return (date.isEqual(startDate) || date.isAfter(startDate))
                && (date.isEqual(today) || date.isBefore(today));
    }

    private Map<LocalDate, DailyChartPoint> buildDailyPoints(List<OrderEntity> orders,
                                                             LocalDate startDate,
                                                             LocalDate today,
                                                             String range) {
        Map<LocalDate, DailyChartPoint> points = new LinkedHashMap<>();

        if ("all".equalsIgnoreCase(range)) {
            orders.stream()
                    .filter(order -> order.getOrderDate() != null)
                    .sorted(Comparator.comparing(OrderEntity::getOrderDate))
                    .forEach(order -> accumulatePoint(points, order.getOrderDate().toLocalDate(), order));
            return points;
        }

        if (startDate == null) {
            startDate = today.minusDays(6);
        }

        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            points.put(date, new DailyChartPoint(0D, 0L));
        }

        orders.stream()
                .filter(order -> order.getOrderDate() != null)
                .forEach(order -> accumulatePoint(points, order.getOrderDate().toLocalDate(), order));

        return points;
    }

    private void accumulatePoint(Map<LocalDate, DailyChartPoint> points, LocalDate date, OrderEntity order) {
        DailyChartPoint current = points.getOrDefault(date, new DailyChartPoint(0D, 0L));

        double revenue = current.revenue()
                + (isRevenueOrder(order) && order.getTotal() != null ? order.getTotal() : 0D);

        long orderCount = current.orders() + 1;

        points.put(date, new DailyChartPoint(revenue, orderCount));
    }

    private boolean isRevenueOrder(OrderEntity order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }

        String status = order.getStatus().trim();
        return "4".equals(status) || "COMPLETED".equalsIgnoreCase(status);
    }

    private boolean isActiveProduct(Product product) {
        return product != null && "1".equals(product.getStatus());
    }

    private boolean isLowStock(Product product) {
        return safeQuantity(product) > 0 && safeQuantity(product) <= 5;
    }

    private long safeQuantity(Product product) {
        try {
            return product != null && product.getQuantity() != null
                    ? Long.parseLong(product.getQuantity().trim())
                    : 0L;
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private AdminDashboardDto.RecentOrderItem toRecentOrderItem(OrderEntity order) {
        return AdminDashboardDto.RecentOrderItem.builder()
                .orderId(order.getOrderId())
                .customerName(order.getAccount() != null && order.getAccount().getName() != null
                        ? order.getAccount().getName()
                        : "Khách hàng")
                .orderDate(order.getOrderDate() != null
                        ? order.getOrderDate().format(DATE_TIME_FORMATTER)
                        : "Chưa cập nhật")
                .total(order.getTotal() != null ? order.getTotal() : 0D)
                .statusLabel(resolveOrderStatus(order.getStatus()))
                .paymentName(order.getPayment() != null && order.getPayment().getPaymentName() != null
                        ? order.getPayment().getPaymentName().toUpperCase(Locale.ROOT)
                        : "N/A")
                .build();
    }

    private AdminDashboardDto.LowStockProductItem toLowStockItem(Product product) {
        return AdminDashboardDto.LowStockProductItem.builder()
                .productId(product.getProductId())
                .productName(product.getProductName() != null ? product.getProductName() : "Sản phẩm")
                .brandName(product.getBrand() != null && product.getBrand().getBrandName() != null
                        ? product.getBrand().getBrandName()
                        : "Chưa rõ")
                .quantity(safeQuantity(product))
                .statusLabel("1".equals(product.getStatus()) ? "Đang bán" : "Tạm ẩn")
                .build();
    }

    private AdminDashboardDto.RecentAccountItem toRecentAccountItem(Account account) {
        return AdminDashboardDto.RecentAccountItem.builder()
                .accountId(account.getAccountId())
                .name(account.getName() != null ? account.getName() : "Tài khoản")
                .email(account.getEmail() != null ? account.getEmail() : "Chưa cập nhật")
                .createdAt(account.getCreateAt() != null
                        ? account.getCreateAt().format(DATE_TIME_FORMATTER)
                        : "Chưa cập nhật")
                .statusLabel("1".equals(account.getStatus()) ? "Hoạt động" : "Tạm khóa")
                .build();
    }

    private String resolveOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Đang xử lý";
        }

        return switch (status) {
            case "0" -> "Chờ thanh toán";
            case "1" -> "Chờ xác nhận";
            case "2" -> "Đang chuẩn bị hàng";
            case "3" -> "Đang giao";
            case "4" -> "Hoàn thành";
            case "5" -> "Đã thanh toán";
            case "6" -> "Đã hủy";
            default -> status;
        };
    }

    private record DailyChartPoint(double revenue, long orders) {
    }
}