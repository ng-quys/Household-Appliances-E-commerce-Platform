package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.RevenueForecastDto;
import com.example.WebBanDoGiaDung.repository.OderDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueForecastService {

    private final OderDetailRepository oderDetailRepository;

    public List<RevenueForecastDto> forecastNextMonths(int numberOfMonths) {
        List<Object[]> rawData = oderDetailRepository.getMonthlyRevenueStatistics();

        List<RevenueForecastDto> result = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        List<YearMonth> months = new ArrayList<>();

        for (Object[] row : rawData) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            double revenue = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();

            YearMonth yearMonth = YearMonth.of(year, month);
            months.add(yearMonth);
            revenues.add(revenue);

            result.add(new RevenueForecastDto(
                    formatMonth(yearMonth),
                    revenue,
                    null
            ));
        }

        if (revenues.isEmpty()) {
            return result;
        }

        YearMonth lastMonth = months.get(months.size() - 1);

        double averageGrowthRate = calculateAverageGrowthRate(revenues);

        double lastRevenue = revenues.get(revenues.size() - 1);

        for (int i = 1; i <= numberOfMonths; i++) {
            YearMonth forecastMonth = lastMonth.plusMonths(i);

            double forecastRevenue = lastRevenue * (1 + averageGrowthRate);

            if (forecastRevenue < 0) {
                forecastRevenue = 0;
            }

            result.add(new RevenueForecastDto(
                    formatMonth(forecastMonth),
                    null,
                    forecastRevenue
            ));

            lastRevenue = forecastRevenue;
        }

        return result;
    }

    private double calculateAverageGrowthRate(List<Double> revenues) {
        if (revenues.size() < 2) {
            return 0.0;
        }

        List<Double> growthRates = new ArrayList<>();

        for (int i = 1; i < revenues.size(); i++) {
            double previous = revenues.get(i - 1);
            double current = revenues.get(i);

            if (previous > 0) {
                double growthRate = (current - previous) / previous;
                growthRates.add(growthRate);
            }
        }

        if (growthRates.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (double rate : growthRates) {
            total += rate;
        }

        double average = total / growthRates.size();

        // Giới hạn để dự báo không bị tăng/giảm quá ảo
        if (average > 0.3) {
            average = 0.3;
        }

        if (average < -0.3) {
            average = -0.3;
        }

        return average;
    }

    private String formatMonth(YearMonth yearMonth) {
        return "Tháng " + yearMonth.getMonthValue() + "/" + yearMonth.getYear();
    }
}