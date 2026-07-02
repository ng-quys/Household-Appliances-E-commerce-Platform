package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.service.LstmRevenueModelService.DailyForecastPoint;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public interface ExcelExportService {
    ByteArrayInputStream exportProducts(List<Product> products);
    ByteArrayInputStream exportOrders(List<OrderEntity> orders);
    ByteArrayInputStream exportRevenueAndForecast(
            Map<java.time.LocalDate, Double> actualRevenue,
            Map<java.time.LocalDate, Long> actualOrders,
            List<DailyForecastPoint> forecasts,
            double forecast7d,
            double forecast30d,
            double forecast6m
    );
}
