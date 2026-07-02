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
import com.example.WebBanDoGiaDung.service.ExcelExportService;
import com.example.WebBanDoGiaDung.service.LstmRevenueModelService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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
    private final ExcelExportService excelExportService;

    public AdminHomeController(ProductRepository productRepository,
                               BrandRepository brandRepository,
                               OrderEntityRepository orderEntityRepository,
                               AccountRepository accountRepository,
                               RevenueForecastService revenueForecastService,
                               ExcelExportService excelExportService) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.orderEntityRepository = orderEntityRepository;
        this.accountRepository = accountRepository;
        this.revenueForecastService = revenueForecastService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/home/export/excel")
    public ResponseEntity<byte[]> exportRevenueReport(@RequestParam(defaultValue = "7days") String range) {
        List<OrderEntity> orders = orderEntityRepository.findAll();
        LocalDate today = LocalDate.now();
        String selectedRange = normalizeRange(range);
        LocalDate startDate = resolveStartDate(selectedRange, today);

        List<OrderEntity> filteredOrders = orders.stream()
                .filter(order -> isWithinRange(order.getOrderDate(), startDate, today, selectedRange))
                .toList();

        Map<LocalDate, DailyChartPoint> dailyPoints = buildDailyPoints(filteredOrders, startDate, today, selectedRange);
        
        Map<LocalDate, Double> actualRevenue = new LinkedHashMap<>();
        Map<LocalDate, Long> actualOrders = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, DailyChartPoint> entry : dailyPoints.entrySet()) {
            actualRevenue.put(entry.getKey(), entry.getValue().revenue());
            actualOrders.put(entry.getKey(), entry.getValue().orders());
        }

        Map<LocalDate, Double> actualRevenueByDate = buildActualRevenueByDate(orders);
        List<LstmRevenueModelService.DailyForecastPoint> rawForecast180DailyPoints =
                revenueForecastService.forecastDailyPoints(180);
        List<LstmRevenueModelService.DailyForecastPoint> forecast180DailyPoints =
                calibrateForecastPoints(rawForecast180DailyPoints, actualRevenueByDate);

        double forecast7Days = forecast180DailyPoints.stream()
                .limit(7)
                .mapToDouble(LstmRevenueModelService.DailyForecastPoint::forecastRevenue)
                .sum();

        double forecast30Days = forecast180DailyPoints.stream()
                .limit(30)
                .mapToDouble(LstmRevenueModelService.DailyForecastPoint::forecastRevenue)
                .sum();

        double forecast6Months = forecast180DailyPoints.stream()
                .mapToDouble(LstmRevenueModelService.DailyForecastPoint::forecastRevenue)
                .sum();

        java.io.ByteArrayInputStream in = excelExportService.exportRevenueAndForecast(
                actualRevenue,
                actualOrders,
                forecast180DailyPoints,
                forecast7Days,
                forecast30Days,
                forecast6Months
        );

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=revenue_report_" + selectedRange + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
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

        // Dữ liệu cho chức năng dự báo doanh thu bằng LSTM.
        // LSTM trả về dự báo theo ngày. Để biểu đồ dễ đọc trên dashboard,
        // phần dự báo được hiệu chỉnh theo mức doanh thu thực tế gần nhất.
        Map<LocalDate, Double> actualRevenueByDate = buildActualRevenueByDate(orders);
        Map<YearMonth, Double> actualRevenueByMonth = buildActualRevenueByMonth(orders);

        List<LstmRevenueModelService.DailyForecastPoint> rawForecast180DailyPoints =
                revenueForecastService.forecastDailyPoints(180);

        List<LstmRevenueModelService.DailyForecastPoint> forecast180DailyPoints =
                calibrateForecastPoints(rawForecast180DailyPoints, actualRevenueByDate);

        double forecast7Days = forecast180DailyPoints.stream()
                .limit(7)
                .mapToDouble(LstmRevenueModelService.DailyForecastPoint::forecastRevenue)
                .sum();

        double forecast30Days = forecast180DailyPoints.stream()
                .limit(30)
                .mapToDouble(LstmRevenueModelService.DailyForecastPoint::forecastRevenue)
                .sum();

        double forecast6Months = forecast180DailyPoints.stream()
                .mapToDouble(LstmRevenueModelService.DailyForecastPoint::forecastRevenue)
                .sum();

        ForecastChartData forecast7ChartData = buildDailyForecastChartData(
                7,
                actualRevenueByDate,
                forecast180DailyPoints
        );

        ForecastChartData forecast30ChartData = buildDailyForecastChartData(
                30,
                actualRevenueByDate,
                forecast180DailyPoints
        );

        ForecastChartData forecast6MonthChartData = buildMonthlyForecastChartData(
                6,
                actualRevenueByMonth,
                forecast180DailyPoints
        );

        model.addAttribute("forecastData", revenueForecastService.forecastNextMonths(6));
        model.addAttribute("forecast7Days", forecast7Days);
        model.addAttribute("forecast30Days", forecast30Days);
        model.addAttribute("forecast6Months", forecast6Months);

        model.addAttribute("forecast7ChartLabels", forecast7ChartData.labels());
        model.addAttribute("forecast7ActualValues", forecast7ChartData.actualValues());
        model.addAttribute("forecast7ChartValues", forecast7ChartData.forecastValues());

        model.addAttribute("forecast30ChartLabels", forecast30ChartData.labels());
        model.addAttribute("forecast30ActualValues", forecast30ChartData.actualValues());
        model.addAttribute("forecast30ChartValues", forecast30ChartData.forecastValues());

        model.addAttribute("forecast6MonthChartLabels", forecast6MonthChartData.labels());
        model.addAttribute("forecast6MonthActualValues", forecast6MonthChartData.actualValues());
        model.addAttribute("forecast6MonthChartValues", forecast6MonthChartData.forecastValues());

        return "admin/home/index";
    }


    private List<LstmRevenueModelService.DailyForecastPoint> calibrateForecastPoints(
            List<LstmRevenueModelService.DailyForecastPoint> rawForecastPoints,
            Map<LocalDate, Double> actualRevenueByDate) {
        if (rawForecastPoints == null || rawForecastPoints.isEmpty()) {
            return List.of();
        }

        LocalDate firstForecastDate = rawForecastPoints.get(0).date();
        LocalDate actualEndDate = firstForecastDate.minusDays(1);

        double recentActualAverage = calculateRecentActualAverage(actualRevenueByDate, actualEndDate, 30);
        double recentActualMax = calculateRecentActualMax(actualRevenueByDate, actualEndDate, 30);
        double rawForecastAverage = rawForecastPoints.stream()
                .limit(30)
                .mapToDouble(LstmRevenueModelService.DailyForecastPoint::forecastRevenue)
                .filter(value -> value > 0)
                .average()
                .orElse(0D);

        if (recentActualAverage <= 0 || rawForecastAverage <= 0) {
            return rawForecastPoints;
        }

        double calibrationFactor = recentActualAverage / rawForecastAverage;

        if (calibrationFactor < 0.10) {
            calibrationFactor = 0.10;
        }

        if (calibrationFactor > 1000) {
            calibrationFactor = 1000;
        }

        double dailyUpperLimit = Math.max(recentActualAverage * 3.0, recentActualMax * 1.25);

        List<LstmRevenueModelService.DailyForecastPoint> calibratedPoints = new ArrayList<>();

        for (LstmRevenueModelService.DailyForecastPoint point : rawForecastPoints) {
            double calibratedRevenue = point.forecastRevenue() * calibrationFactor;

            if (dailyUpperLimit > 0 && calibratedRevenue > dailyUpperLimit) {
                calibratedRevenue = dailyUpperLimit;
            }

            if (calibratedRevenue < 0) {
                calibratedRevenue = 0;
            }

            calibratedPoints.add(new LstmRevenueModelService.DailyForecastPoint(
                    point.date(),
                    calibratedRevenue
            ));
        }

        return calibratedPoints;
    }

    private double calculateRecentActualAverage(Map<LocalDate, Double> actualRevenueByDate,
                                                LocalDate endDate,
                                                int numberOfDays) {
        if (actualRevenueByDate == null || actualRevenueByDate.isEmpty() || endDate == null) {
            return 0D;
        }

        double totalRevenue = 0D;
        int countedDays = 0;

        LocalDate startDate = endDate.minusDays(numberOfDays - 1L);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            totalRevenue += actualRevenueByDate.getOrDefault(date, 0D);
            countedDays++;
        }

        if (countedDays == 0) {
            return 0D;
        }

        double averageIncludingZeroDays = totalRevenue / countedDays;

        if (averageIncludingZeroDays > 0) {
            return averageIncludingZeroDays;
        }

        return actualRevenueByDate.values().stream()
                .mapToDouble(value -> value != null ? value : 0D)
                .filter(value -> value > 0)
                .average()
                .orElse(0D);
    }

    private double calculateRecentActualMax(Map<LocalDate, Double> actualRevenueByDate,
                                            LocalDate endDate,
                                            int numberOfDays) {
        if (actualRevenueByDate == null || actualRevenueByDate.isEmpty() || endDate == null) {
            return 0D;
        }

        double maxRevenue = 0D;
        LocalDate startDate = endDate.minusDays(numberOfDays - 1L);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            double revenue = actualRevenueByDate.getOrDefault(date, 0D);

            if (revenue > maxRevenue) {
                maxRevenue = revenue;
            }
        }

        return maxRevenue;
    }

    private ForecastChartData buildDailyForecastChartData(int numberOfDays,
                                                          Map<LocalDate, Double> actualRevenueByDate,
                                                          List<LstmRevenueModelService.DailyForecastPoint> forecastPoints) {
        List<String> labels = new ArrayList<>();
        List<Double> actualValues = new ArrayList<>();
        List<Double> forecastValues = new ArrayList<>();

        if (forecastPoints == null || forecastPoints.isEmpty()) {
            return new ForecastChartData(labels, actualValues, forecastValues);
        }

        LocalDate firstForecastDate = forecastPoints.get(0).date();
        LocalDate actualEndDate = firstForecastDate.minusDays(1);
        LocalDate actualStartDate = actualEndDate.minusDays(numberOfDays - 1L);

        for (LocalDate date = actualStartDate; !date.isAfter(actualEndDate); date = date.plusDays(1)) {
            labels.add(date.format(DAY_LABEL_FORMATTER));
            actualValues.add(actualRevenueByDate.getOrDefault(date, 0D));
            forecastValues.add(null);
        }

        forecastPoints.stream()
                .limit(numberOfDays)
                .forEach(point -> {
                    labels.add(point.date().format(DAY_LABEL_FORMATTER));
                    actualValues.add(null);
                    forecastValues.add(point.forecastRevenue());
                });

        return new ForecastChartData(labels, actualValues, forecastValues);
    }

    private ForecastChartData buildMonthlyForecastChartData(int numberOfMonths,
                                                            Map<YearMonth, Double> actualRevenueByMonth,
                                                            List<LstmRevenueModelService.DailyForecastPoint> forecastPoints) {
        List<String> labels = new ArrayList<>();
        List<Double> actualValues = new ArrayList<>();
        List<Double> forecastValues = new ArrayList<>();

        if (forecastPoints == null || forecastPoints.isEmpty()) {
            return new ForecastChartData(labels, actualValues, forecastValues);
        }

        YearMonth firstForecastMonth = YearMonth.from(forecastPoints.get(0).date());
        YearMonth actualStartMonth = firstForecastMonth.minusMonths(numberOfMonths);

        for (int i = 0; i < numberOfMonths; i++) {
            YearMonth month = actualStartMonth.plusMonths(i);
            labels.add(formatMonthLabel(month));
            actualValues.add(actualRevenueByMonth.getOrDefault(month, 0D));
            forecastValues.add(null);
        }

        Map<YearMonth, Double> forecastByMonth = new LinkedHashMap<>();

        for (LstmRevenueModelService.DailyForecastPoint point : forecastPoints) {
            YearMonth forecastMonth = YearMonth.from(point.date());

            if (!forecastByMonth.containsKey(forecastMonth) && forecastByMonth.size() >= numberOfMonths) {
                break;
            }

            forecastByMonth.merge(forecastMonth, point.forecastRevenue(), Double::sum);
        }

        for (Map.Entry<YearMonth, Double> entry : forecastByMonth.entrySet()) {
            labels.add(formatMonthLabel(entry.getKey()));
            actualValues.add(null);
            forecastValues.add(entry.getValue());
        }

        return new ForecastChartData(labels, actualValues, forecastValues);
    }

    private Map<LocalDate, Double> buildActualRevenueByDate(List<OrderEntity> orders) {
        Map<LocalDate, Double> actualRevenueByDate = new LinkedHashMap<>();

        orders.stream()
                .filter(this::isRevenueOrder)
                .filter(order -> order.getOrderDate() != null)
                .forEach(order -> actualRevenueByDate.merge(
                        order.getOrderDate().toLocalDate(),
                        order.getTotal() != null ? order.getTotal() : 0D,
                        Double::sum
                ));

        return actualRevenueByDate;
    }

    private Map<YearMonth, Double> buildActualRevenueByMonth(List<OrderEntity> orders) {
        Map<YearMonth, Double> actualRevenueByMonth = new LinkedHashMap<>();

        orders.stream()
                .filter(this::isRevenueOrder)
                .filter(order -> order.getOrderDate() != null)
                .forEach(order -> actualRevenueByMonth.merge(
                        YearMonth.from(order.getOrderDate()),
                        order.getTotal() != null ? order.getTotal() : 0D,
                        Double::sum
                ));

        return actualRevenueByMonth;
    }

    private String formatMonthLabel(YearMonth yearMonth) {
        return "Tháng " + yearMonth.getMonthValue() + "/" + yearMonth.getYear();
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

    private record ForecastChartData(List<String> labels, List<Double> actualValues, List<Double> forecastValues) {
    }

    private record DailyChartPoint(double revenue, long orders) {
    }
}