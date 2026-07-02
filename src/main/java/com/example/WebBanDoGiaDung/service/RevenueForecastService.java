package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.RevenueForecastDto;
import com.example.WebBanDoGiaDung.repository.OderDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RevenueForecastService {

    private final OderDetailRepository oderDetailRepository;
    private final LstmRevenueModelService lstmRevenueModelService;

    public List<RevenueForecastDto> forecastNextMonths(int numberOfMonths) {
        Map<YearMonth, RevenueForecastDto> resultMap = new LinkedHashMap<>();

        List<Object[]> rawData = oderDetailRepository.getMonthlyRevenueStatistics();
        List<Double> historicalRevenues = new ArrayList<>();
        List<YearMonth> historicalMonths = new ArrayList<>();

        for (Object[] row : rawData) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            double revenue = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();

            YearMonth yearMonth = YearMonth.of(year, month);
            historicalMonths.add(yearMonth);
            historicalRevenues.add(revenue);

            resultMap.put(yearMonth, new RevenueForecastDto(
                    formatMonth(yearMonth),
                    revenue,
                    null
            ));
        }

        if (lstmRevenueModelService.isAvailable()) {
            addLstmMonthlyForecast(resultMap, numberOfMonths);
            return new ArrayList<>(resultMap.values());
        }

        addFallbackForecast(resultMap, historicalMonths, historicalRevenues, numberOfMonths);
        return new ArrayList<>(resultMap.values());
    }

    public List<LstmRevenueModelService.DailyForecastPoint> forecastDailyPoints(int days) {
        if (!lstmRevenueModelService.isAvailable() || days <= 0) {
            return List.of();
        }

        return lstmRevenueModelService.forecastNextDays(days);
    }

    public double forecastTotalNextDays(int days) {
        return forecastDailyPoints(days)
                .stream()
                .mapToDouble(LstmRevenueModelService.DailyForecastPoint::forecastRevenue)
                .sum();
    }

    private void addLstmMonthlyForecast(Map<YearMonth, RevenueForecastDto> resultMap, int numberOfMonths) {
        List<LstmRevenueModelService.DailyForecastPoint> dailyForecasts =
                lstmRevenueModelService.forecastNextDays(numberOfMonths * 31);

        Map<YearMonth, Double> forecastByMonth = new LinkedHashMap<>();

        for (LstmRevenueModelService.DailyForecastPoint point : dailyForecasts) {
            YearMonth yearMonth = YearMonth.from(point.date());

            if (!forecastByMonth.containsKey(yearMonth) && forecastByMonth.size() >= numberOfMonths) {
                break;
            }

            forecastByMonth.merge(yearMonth, point.forecastRevenue(), Double::sum);
        }

        for (Map.Entry<YearMonth, Double> entry : forecastByMonth.entrySet()) {
            YearMonth yearMonth = entry.getKey();
            double forecastRevenue = entry.getValue();

            RevenueForecastDto existing = resultMap.get(yearMonth);

            if (existing != null) {
                existing.setForecastRevenue(forecastRevenue);
            } else {
                resultMap.put(yearMonth, new RevenueForecastDto(
                        formatMonth(yearMonth),
                        null,
                        forecastRevenue
                ));
            }
        }
    }

    private void addFallbackForecast(Map<YearMonth, RevenueForecastDto> resultMap,
                                     List<YearMonth> historicalMonths,
                                     List<Double> historicalRevenues,
                                     int numberOfMonths) {
        if (historicalRevenues.isEmpty()) {
            return;
        }

        YearMonth lastMonth = historicalMonths.get(historicalMonths.size() - 1);
        double averageGrowthRate = calculateAverageGrowthRate(historicalRevenues);
        double lastRevenue = historicalRevenues.get(historicalRevenues.size() - 1);

        for (int i = 1; i <= numberOfMonths; i++) {
            YearMonth forecastMonth = lastMonth.plusMonths(i);
            double forecastRevenue = lastRevenue * (1 + averageGrowthRate);

            if (forecastRevenue < 0) {
                forecastRevenue = 0;
            }

            resultMap.put(forecastMonth, new RevenueForecastDto(
                    formatMonth(forecastMonth),
                    null,
                    forecastRevenue
            ));

            lastRevenue = forecastRevenue;
        }
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
                growthRates.add((current - previous) / previous);
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
