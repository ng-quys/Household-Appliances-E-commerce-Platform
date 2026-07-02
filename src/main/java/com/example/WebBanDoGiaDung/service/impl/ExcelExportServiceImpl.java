package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.OrderEntity;
import com.example.WebBanDoGiaDung.entity.Product;
import com.example.WebBanDoGiaDung.service.ExcelExportService;
import com.example.WebBanDoGiaDung.service.LstmRevenueModelService.DailyForecastPoint;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ByteArrayInputStream exportProducts(List<Product> products) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách sản phẩm");

            // Fonts & Styles
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Segoe UI");
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCellStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerCellStyle.setBorderLeft(BorderStyle.THIN);
            headerCellStyle.setBorderRight(BorderStyle.THIN);
            headerCellStyle.setBorderTop(BorderStyle.THIN);

            Font dataFont = workbook.createFont();
            dataFont.setFontName("Segoe UI");

            CellStyle dataCellStyle = workbook.createCellStyle();
            dataCellStyle.setFont(dataFont);
            dataCellStyle.setBorderBottom(BorderStyle.THIN);
            dataCellStyle.setBorderLeft(BorderStyle.THIN);
            dataCellStyle.setBorderRight(BorderStyle.THIN);
            dataCellStyle.setBorderTop(BorderStyle.THIN);

            CellStyle currencyCellStyle = workbook.createCellStyle();
            currencyCellStyle.cloneStyleFrom(dataCellStyle);
            DataFormat format = workbook.createDataFormat();
            currencyCellStyle.setDataFormat(format.getFormat("#,##0\" đ\""));
            currencyCellStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.cloneStyleFrom(dataCellStyle);
            dateCellStyle.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));
            dateCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Row chẵn màu nền xám nhạt để dễ nhìn (zebra striping)
            CellStyle zebraCellStyle = workbook.createCellStyle();
            zebraCellStyle.cloneStyleFrom(dataCellStyle);
            zebraCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            zebraCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle zebraCurrencyCellStyle = workbook.createCellStyle();
            zebraCurrencyCellStyle.cloneStyleFrom(currencyCellStyle);
            zebraCurrencyCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            zebraCurrencyCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle zebraDateCellStyle = workbook.createCellStyle();
            zebraDateCellStyle.cloneStyleFrom(dateCellStyle);
            zebraDateCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            zebraDateCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            String[] columns = {"ID", "Tên sản phẩm", "Giá", "Tồn kho", "Thương hiệu", "Thể loại", "Trạng thái", "Ngày tạo", "Người tạo"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIdx++);
                boolean isEven = (rowIdx % 2 == 0);

                CellStyle currentStyle = isEven ? zebraCellStyle : dataCellStyle;
                CellStyle currentCurrencyStyle = isEven ? zebraCurrencyCellStyle : currencyCellStyle;
                CellStyle currentDateStyle = isEven ? zebraDateCellStyle : dateCellStyle;

                // ID
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(product.getProductId());
                cell0.setCellStyle(currentStyle);

                // Name
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(product.getProductName() != null ? product.getProductName() : "");
                cell1.setCellStyle(currentStyle);

                // Price
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(product.getPrice() != null ? product.getPrice() : 0.0);
                cell2.setCellStyle(currentCurrencyStyle);

                // Quantity
                Cell cell3 = row.createCell(3);
                int qty = 0;
                try {
                    qty = Integer.parseInt(product.getQuantity().trim());
                } catch (Exception ignored) {}
                cell3.setCellValue(qty);
                cell3.setCellStyle(currentStyle);

                // Brand
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(product.getBrand() != null && product.getBrand().getBrandName() != null 
                        ? product.getBrand().getBrandName() : "");
                cell4.setCellStyle(currentStyle);

                // Genre
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(product.getGenre() != null && product.getGenre().getGenreName() != null 
                        ? product.getGenre().getGenreName() : "");
                cell5.setCellStyle(currentStyle);

                // Status
                Cell cell6 = row.createCell(6);
                cell6.setCellValue("1".equals(product.getStatus()) ? "Đang bán" : "Tạm ẩn");
                cell6.setCellStyle(currentStyle);

                // Create At
                Cell cell7 = row.createCell(7);
                if (product.getCreateAt() != null) {
                    cell7.setCellValue(product.getCreateAt().format(DATE_TIME_FORMATTER));
                } else {
                    cell7.setCellValue("");
                }
                cell7.setCellStyle(currentStyle);

                // Create By
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(product.getCreateBy() != null ? product.getCreateBy() : "");
                cell8.setCellStyle(currentStyle);
            }

            // Auto-size columns & Auto-filter
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            if (rowIdx > 1) {
                sheet.setAutoFilter(new CellRangeAddress(0, rowIdx - 1, 0, columns.length - 1));
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi sinh file Excel danh sách sản phẩm: " + e.getMessage(), e);
        }
    }

    @Override
    public ByteArrayInputStream exportOrders(List<OrderEntity> orders) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách đơn hàng");

            // Fonts & Styles
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Segoe UI");
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCellStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerCellStyle.setBorderLeft(BorderStyle.THIN);
            headerCellStyle.setBorderRight(BorderStyle.THIN);
            headerCellStyle.setBorderTop(BorderStyle.THIN);

            Font dataFont = workbook.createFont();
            dataFont.setFontName("Segoe UI");

            CellStyle dataCellStyle = workbook.createCellStyle();
            dataCellStyle.setFont(dataFont);
            dataCellStyle.setBorderBottom(BorderStyle.THIN);
            dataCellStyle.setBorderLeft(BorderStyle.THIN);
            dataCellStyle.setBorderRight(BorderStyle.THIN);
            dataCellStyle.setBorderTop(BorderStyle.THIN);

            CellStyle currencyCellStyle = workbook.createCellStyle();
            currencyCellStyle.cloneStyleFrom(dataCellStyle);
            DataFormat format = workbook.createDataFormat();
            currencyCellStyle.setDataFormat(format.getFormat("#,##0\" đ\""));
            currencyCellStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle zebraCellStyle = workbook.createCellStyle();
            zebraCellStyle.cloneStyleFrom(dataCellStyle);
            zebraCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            zebraCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle zebraCurrencyCellStyle = workbook.createCellStyle();
            zebraCurrencyCellStyle.cloneStyleFrom(currencyCellStyle);
            zebraCurrencyCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            zebraCurrencyCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            String[] columns = {"Mã đơn", "Khách hàng", "Email", "Số điện thoại", "Ngày đặt", "Thanh toán", "Vận chuyển", "Tổng tiền", "Trạng thái"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            for (OrderEntity order : orders) {
                Row row = sheet.createRow(rowIdx++);
                boolean isEven = (rowIdx % 2 == 0);

                CellStyle currentStyle = isEven ? zebraCellStyle : dataCellStyle;
                CellStyle currentCurrencyStyle = isEven ? zebraCurrencyCellStyle : currencyCellStyle;

                // ID
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(order.getOrderId());
                cell0.setCellStyle(currentStyle);

                // Customer Name
                Cell cell1 = row.createCell(1);
                String name = "Khách hàng";
                if (order.getOrderAddress() != null && order.getOrderAddress().getOrderUsername() != null) {
                    name = order.getOrderAddress().getOrderUsername();
                } else if (order.getAccount() != null && order.getAccount().getName() != null) {
                    name = order.getAccount().getName();
                }
                cell1.setCellValue(name);
                cell1.setCellStyle(currentStyle);

                // Email
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(order.getAccount() != null && order.getAccount().getEmail() != null 
                        ? order.getAccount().getEmail() : "Chưa cập nhật");
                cell2.setCellStyle(currentStyle);

                // Phone
                Cell cell3 = row.createCell(3);
                String phone = "Chưa cập nhật";
                if (order.getOrderAddress() != null && order.getOrderAddress().getOrderPhoneNumber() != null) {
                    phone = order.getOrderAddress().getOrderPhoneNumber();
                } else if (order.getAccount() != null && order.getAccount().getPhone() != null) {
                    phone = order.getAccount().getPhone();
                }
                cell3.setCellValue(phone);
                cell3.setCellStyle(currentStyle);

                // Order Date
                Cell cell4 = row.createCell(4);
                if (order.getOrderDate() != null) {
                    cell4.setCellValue(order.getOrderDate().format(DATE_TIME_FORMATTER));
                } else {
                    cell4.setCellValue("N/A");
                }
                cell4.setCellStyle(currentStyle);

                // Payment
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(order.getPayment() != null && order.getPayment().getPaymentName() != null 
                        ? order.getPayment().getPaymentName().toUpperCase() : "N/A");
                cell5.setCellStyle(currentStyle);

                // Delivery
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(order.getDelivery() != null && order.getDelivery().getDeliveryName() != null 
                        ? order.getDelivery().getDeliveryName() : "N/A");
                cell6.setCellStyle(currentStyle);

                // Total
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(order.getTotal() != null ? order.getTotal() : 0.0);
                cell7.setCellStyle(currentCurrencyStyle);

                // Status
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(resolveOrderStatus(order.getStatus()));
                cell8.setCellStyle(currentStyle);
            }

            // Auto-size columns & Auto-filter
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            if (rowIdx > 1) {
                sheet.setAutoFilter(new CellRangeAddress(0, rowIdx - 1, 0, columns.length - 1));
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi sinh file Excel danh sách đơn hàng: " + e.getMessage(), e);
        }
    }

    @Override
    public ByteArrayInputStream exportRevenueAndForecast(
            Map<LocalDate, Double> actualRevenue,
            Map<LocalDate, Long> actualOrders,
            List<DailyForecastPoint> forecasts,
            double forecast7d,
            double forecast30d,
            double forecast6m) {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // ================= SHEET 1: DOANH THU THEO NGÀY =================
            Sheet sheet1 = workbook.createSheet("Doanh thu theo ngày");

            // Fonts & Styles
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Segoe UI");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Segoe UI");
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCellStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerCellStyle.setBorderLeft(BorderStyle.THIN);
            headerCellStyle.setBorderRight(BorderStyle.THIN);
            headerCellStyle.setBorderTop(BorderStyle.THIN);

            Font dataFont = workbook.createFont();
            dataFont.setFontName("Segoe UI");

            CellStyle dataCellStyle = workbook.createCellStyle();
            dataCellStyle.setFont(dataFont);
            dataCellStyle.setBorderBottom(BorderStyle.THIN);
            dataCellStyle.setBorderLeft(BorderStyle.THIN);
            dataCellStyle.setBorderRight(BorderStyle.THIN);
            dataCellStyle.setBorderTop(BorderStyle.THIN);

            CellStyle currencyCellStyle = workbook.createCellStyle();
            currencyCellStyle.cloneStyleFrom(dataCellStyle);
            DataFormat format = workbook.createDataFormat();
            currencyCellStyle.setDataFormat(format.getFormat("#,##0\" đ\""));
            currencyCellStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle zebraCellStyle = workbook.createCellStyle();
            zebraCellStyle.cloneStyleFrom(dataCellStyle);
            zebraCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            zebraCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle zebraCurrencyCellStyle = workbook.createCellStyle();
            zebraCurrencyCellStyle.cloneStyleFrom(currencyCellStyle);
            zebraCurrencyCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            zebraCurrencyCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Title
            Row titleRow = sheet1.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DOANH THU THỰC TẾ THEO NGÀY");
            titleCell.setCellStyle(titleStyle);

            // Headers
            String[] sheet1Cols = {"Ngày", "Tháng", "Số đơn hàng", "Doanh thu"};
            Row headerRow1 = sheet1.createRow(2);
            for (int i = 0; i < sheet1Cols.length; i++) {
                Cell cell = headerRow1.createCell(i);
                cell.setCellValue(sheet1Cols[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx1 = 3;
            double totalActualRevenue = 0;
            long totalActualOrders = 0;

            for (LocalDate date : actualRevenue.keySet()) {
                Row row = sheet1.createRow(rowIdx1++);
                boolean isEven = (rowIdx1 % 2 == 0);

                CellStyle currentStyle = isEven ? zebraCellStyle : dataCellStyle;
                CellStyle currentCurrencyStyle = isEven ? zebraCurrencyCellStyle : currencyCellStyle;

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(date.format(DATE_FORMATTER));
                cell0.setCellStyle(currentStyle);

                Cell cell1 = row.createCell(1);
                java.time.YearMonth ym = java.time.YearMonth.from(date);
                cell1.setCellValue("Tháng " + ym.getMonthValue() + "/" + ym.getYear());
                cell1.setCellStyle(currentStyle);

                long orders = actualOrders.getOrDefault(date, 0L);
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(orders);
                cell2.setCellStyle(currentStyle);
                totalActualOrders += orders;

                double rev = actualRevenue.getOrDefault(date, 0.0);
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(rev);
                cell3.setCellStyle(currentCurrencyStyle);
                totalActualRevenue += rev;
            }

            // Summary Row
            Row sumRow1 = sheet1.createRow(rowIdx1);
            Cell sumCell0 = sumRow1.createCell(0);
            sumCell0.setCellValue("Tổng cộng");
            Font boldDataFont = workbook.createFont();
            boldDataFont.setFontName("Segoe UI");
            boldDataFont.setBold(true);
            CellStyle boldDataCellStyle = workbook.createCellStyle();
            boldDataCellStyle.cloneStyleFrom(dataCellStyle);
            boldDataCellStyle.setFont(boldDataFont);
            sumCell0.setCellStyle(boldDataCellStyle);

            Cell sumCell1 = sumRow1.createCell(1);
            sumCell1.setCellValue("");
            sumCell1.setCellStyle(boldDataCellStyle);

            Cell sumCell2 = sumRow1.createCell(2);
            sumCell2.setCellValue(totalActualOrders);
            sumCell2.setCellStyle(boldDataCellStyle);

            Cell sumCell3 = sumRow1.createCell(3);
            sumCell3.setCellValue(totalActualRevenue);
            CellStyle boldCurrencyStyle = workbook.createCellStyle();
            boldCurrencyStyle.cloneStyleFrom(currencyCellStyle);
            boldCurrencyStyle.setFont(boldDataFont);
            sumCell3.setCellStyle(boldCurrencyStyle);

            // Auto-filter on Sheet 1
            if (rowIdx1 > 3) {
                sheet1.setAutoFilter(new CellRangeAddress(2, rowIdx1 - 1, 0, 3));
            }

            sheet1.autoSizeColumn(0);
            sheet1.autoSizeColumn(1);
            sheet1.autoSizeColumn(2);
            sheet1.autoSizeColumn(3);


            // ================= SHEET 2: DOANH THU THEO THÁNG =================
            Sheet sheet2 = workbook.createSheet("Doanh thu theo tháng");

            // Title
            Row titleRow2 = sheet2.createRow(0);
            Cell titleCell2 = titleRow2.createCell(0);
            titleCell2.setCellValue("BÁO CÁO DOANH THU THEO THÁNG");
            titleCell2.setCellStyle(titleStyle);

            // Headers
            String[] sheet2Cols = {"Tháng", "Số đơn hàng", "Doanh thu"};
            Row headerRow2 = sheet2.createRow(2);
            for (int i = 0; i < sheet2Cols.length; i++) {
                Cell cell = headerRow2.createCell(i);
                cell.setCellValue(sheet2Cols[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Group data by Month
            Map<java.time.YearMonth, Double> monthlyRevenue = new java.util.LinkedHashMap<>();
            Map<java.time.YearMonth, Long> monthlyOrders = new java.util.LinkedHashMap<>();

            for (Map.Entry<LocalDate, Double> entry : actualRevenue.entrySet()) {
                java.time.YearMonth ym = java.time.YearMonth.from(entry.getKey());
                monthlyRevenue.put(ym, monthlyRevenue.getOrDefault(ym, 0.0) + entry.getValue());
            }
            for (Map.Entry<LocalDate, Long> entry : actualOrders.entrySet()) {
                java.time.YearMonth ym = java.time.YearMonth.from(entry.getKey());
                monthlyOrders.put(ym, monthlyOrders.getOrDefault(ym, 0L) + entry.getValue());
            }

            int rowIdx2 = 3;
            double totalMonthlyRevenue = 0;
            long totalMonthlyOrders = 0;

            for (java.time.YearMonth ym : monthlyRevenue.keySet()) {
                Row row = sheet2.createRow(rowIdx2++);
                boolean isEven = (rowIdx2 % 2 == 0);

                CellStyle currentStyle = isEven ? zebraCellStyle : dataCellStyle;
                CellStyle currentCurrencyStyle = isEven ? zebraCurrencyCellStyle : currencyCellStyle;

                Cell cell0 = row.createCell(0);
                cell0.setCellValue("Tháng " + ym.getMonthValue() + "/" + ym.getYear());
                cell0.setCellStyle(currentStyle);

                long orders = monthlyOrders.getOrDefault(ym, 0L);
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(orders);
                cell1.setCellStyle(currentStyle);
                totalMonthlyOrders += orders;

                double rev = monthlyRevenue.getOrDefault(ym, 0.0);
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(rev);
                cell2.setCellStyle(currentCurrencyStyle);
                totalMonthlyRevenue += rev;
            }

            // Summary Row for Sheet 2
            Row sumRow2 = sheet2.createRow(rowIdx2);
            Cell sumCell2_0 = sumRow2.createCell(0);
            sumCell2_0.setCellValue("Tổng cộng");
            sumCell2_0.setCellStyle(boldDataCellStyle);

            Cell sumCell2_1 = sumRow2.createCell(1);
            sumCell2_1.setCellValue(totalMonthlyOrders);
            sumCell2_1.setCellStyle(boldDataCellStyle);

            Cell sumCell2_2 = sumRow2.createCell(2);
            sumCell2_2.setCellValue(totalMonthlyRevenue);
            sumCell2_2.setCellStyle(boldCurrencyStyle);

            // Auto-filter on Sheet 2
            if (rowIdx2 > 3) {
                sheet2.setAutoFilter(new CellRangeAddress(2, rowIdx2 - 1, 0, 2));
            }

            sheet2.autoSizeColumn(0);
            sheet2.autoSizeColumn(1);
            sheet2.autoSizeColumn(2);


            // ================= SHEET 3: DỰ BÁO LSTM =================
            Sheet sheet3 = workbook.createSheet("Dự báo doanh thu LSTM");

            // Title
            Row titleRow3 = sheet3.createRow(0);
            Cell titleCell3 = titleRow3.createCell(0);
            titleCell3.setCellValue("BÁO CÁO DỰ BÁO DOANH THU LSTM (ONNX AI)");
            titleCell3.setCellStyle(titleStyle);

            // KPI Cards in Excel Sheet 3
            Font cardTitleFont = workbook.createFont();
            cardTitleFont.setFontName("Segoe UI");
            cardTitleFont.setBold(true);
            cardTitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            
            CellStyle cardTitleStyle = workbook.createCellStyle();
            cardTitleStyle.setFont(cardTitleFont);
            cardTitleStyle.setAlignment(HorizontalAlignment.LEFT);

            Font cardValFont = workbook.createFont();
            cardValFont.setFontName("Segoe UI");
            cardValFont.setBold(true);
            cardValFont.setFontHeightInPoints((short) 12);
            cardValFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle cardValStyle = workbook.createCellStyle();
            cardValStyle.setFont(cardValFont);
            cardValStyle.setDataFormat(format.getFormat("#,##0\" đ\""));
            cardValStyle.setAlignment(HorizontalAlignment.LEFT);

            Row kpiTitleRow = sheet3.createRow(2);
            Cell kpi7Title = kpiTitleRow.createCell(0);
            kpi7Title.setCellValue("DỰ BÁO 7 NGÀY TỚI");
            kpi7Title.setCellStyle(cardTitleStyle);

            Cell kpi30Title = kpiTitleRow.createCell(2);
            kpi30Title.setCellValue("DỰ BÁO 30 NGÀY TỚI");
            kpi30Title.setCellStyle(cardTitleStyle);

            Cell kpi180Title = kpiTitleRow.createCell(4);
            kpi180Title.setCellValue("DỰ BÁO 6 THÁNG TỚI");
            kpi180Title.setCellStyle(cardTitleStyle);

            Row kpiValRow = sheet3.createRow(3);
            Cell kpi7Val = kpiValRow.createCell(0);
            kpi7Val.setCellValue(forecast7d);
            kpi7Val.setCellStyle(cardValStyle);

            Cell kpi30Val = kpiValRow.createCell(2);
            kpi30Val.setCellValue(forecast30d);
            kpi30Val.setCellStyle(cardValStyle);

            Cell kpi180Val = kpiValRow.createCell(4);
            kpi180Val.setCellValue(forecast6m);
            kpi180Val.setCellStyle(cardValStyle);

            // Table Headers for Forecasts
            String[] sheet3Cols = {"Ngày dự báo", "Doanh thu dự báo (VND)"};
            Row headerRow3 = sheet3.createRow(5);
            for (int i = 0; i < sheet3Cols.length; i++) {
                Cell cell = headerRow3.createCell(i);
                cell.setCellValue(sheet3Cols[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx3 = 6;
            for (DailyForecastPoint point : forecasts) {
                Row row = sheet3.createRow(rowIdx3++);
                boolean isEven = (rowIdx3 % 2 == 0);

                CellStyle currentStyle = isEven ? zebraCellStyle : dataCellStyle;
                CellStyle currentCurrencyStyle = isEven ? zebraCurrencyCellStyle : currencyCellStyle;

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(point.date().format(DATE_FORMATTER));
                cell0.setCellStyle(currentStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(point.forecastRevenue());
                cell1.setCellStyle(currentCurrencyStyle);
            }

            // Auto-filter on Sheet 3
            if (rowIdx3 > 6) {
                sheet3.setAutoFilter(new CellRangeAddress(5, rowIdx3 - 1, 0, 1));
            }

            sheet3.autoSizeColumn(0);
            sheet3.autoSizeColumn(1);
            sheet3.autoSizeColumn(2);
            sheet3.autoSizeColumn(3);
            sheet3.autoSizeColumn(4);
            sheet3.autoSizeColumn(5);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi sinh file Excel báo cáo doanh thu & dự báo: " + e.getMessage(), e);
        }
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
}
