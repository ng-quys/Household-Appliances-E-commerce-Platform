package com.example.WebBanDoGiaDung.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminDashboardDto {
    long totalProducts;
    long totalBrands;
    long totalOrders;
    long totalAccounts;
    double totalRevenue;
    long lowStockProducts;
    String selectedRange;
    List<String> chartLabels;
    List<Double> revenueSeries;
    List<Long> orderSeries;
    List<RecentOrderItem> recentOrders;
    List<LowStockProductItem> lowStockItems;
    List<RecentAccountItem> recentAccounts;

    @Value
    @Builder
    public static class RecentOrderItem {
        Integer orderId;
        String customerName;
        String orderDate;
        double total;
        String statusLabel;
        String paymentName;
    }

    @Value
    @Builder
    public static class LowStockProductItem {
        Integer productId;
        String productName;
        String brandName;
        long quantity;
        String statusLabel;
    }

    @Value
    @Builder
    public static class RecentAccountItem {
        Integer accountId;
        String name;
        String email;
        String createdAt;
        String statusLabel;
    }
}
