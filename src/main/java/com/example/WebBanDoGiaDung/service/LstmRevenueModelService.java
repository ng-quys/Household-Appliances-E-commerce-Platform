package com.example.WebBanDoGiaDung.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LstmRevenueModelService implements DisposableBean {

    private static final double EPS = 1e-8;

    private OrtEnvironment environment;
    private OrtSession session;
    private RevenueLstmMeta meta;
    private boolean available = false;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource modelResource = new ClassPathResource("ml/revenue_lstm.onnx");
            ClassPathResource metaResource = new ClassPathResource("ml/revenue_lstm_meta.json");

            byte[] modelBytes;
            try (InputStream inputStream = modelResource.getInputStream()) {
                modelBytes = inputStream.readAllBytes();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            try (InputStream inputStream = metaResource.getInputStream()) {
                meta = objectMapper.readValue(inputStream, RevenueLstmMeta.class);
            }

            environment = OrtEnvironment.getEnvironment();
            try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
                session = environment.createSession(modelBytes, options);
            }

            available = true;
            System.out.println("Đã load LSTM revenue model ONNX thành công.");
            System.out.println("LSTM input name: " + meta.inputName);
            System.out.println("LSTM output name: " + meta.outputName);
        } catch (Throwable exception) {
            available = false;
            System.out.println("Không thể load LSTM revenue model. Ứng dụng vẫn chạy bằng forecast fallback.");
            System.out.println("Nguyên nhân: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        }
    }

    public boolean isAvailable() {
        return available && session != null && meta != null;
    }

    public List<DailyForecastPoint> forecastNextDays(int days) {
        if (!isAvailable() || days <= 0) {
            return List.of();
        }

        try {
            float[][] window = copyLastWindowFeatures();
            LocalDate lastTrainingDate = LocalDate.parse(meta.lastTrainingDate);
            List<DailyForecastPoint> result = new ArrayList<>();

            for (int i = 1; i <= days; i++) {
                LocalDate forecastDate = lastTrainingDate.plusDays(i);

                float predScaled = predictOneDay(window);
                predScaled = clip(predScaled, 0.0f, 1.0f);

                double predRevenue = inverseTransform(predScaled);
                result.add(new DailyForecastPoint(forecastDate, predRevenue));

                float[] newRow = buildFutureFeatureRow(forecastDate, predScaled);
                slideWindow(window, newRow);
            }

            return result;
        } catch (Throwable exception) {
            System.out.println("Lỗi khi dự báo doanh thu bằng LSTM: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            return List.of();
        }
    }

    private float predictOneDay(float[][] window) throws Exception {
        float[][][] inputData = new float[1][meta.windowSize][meta.nFeatures];

        for (int i = 0; i < meta.windowSize; i++) {
            System.arraycopy(window[i], 0, inputData[0][i], 0, meta.nFeatures);
        }

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(environment, inputData);
             OrtSession.Result output = session.run(Collections.singletonMap(meta.inputName, inputTensor))) {

            Object value = output.get(0).getValue();

            if (value instanceof float[][] outputArray) {
                return outputArray[0][0];
            }

            if (value instanceof float[] outputArray) {
                return outputArray[0];
            }

            if (value instanceof float[][][] outputArray) {
                return outputArray[0][0][0];
            }

            throw new IllegalStateException("Không đọc được output từ ONNX model.");
        }
    }

    private float[][] copyLastWindowFeatures() {
        float[][] window = new float[meta.windowSize][meta.nFeatures];

        for (int i = 0; i < meta.windowSize; i++) {
            List<Double> row = meta.lastWindowFeatures.get(i);
            for (int j = 0; j < meta.nFeatures; j++) {
                window[i][j] = row.get(j).floatValue();
            }
        }

        return window;
    }

    private float[] buildFutureFeatureRow(LocalDate date, float revenueScaledValue) {
        int timeIndex = (int) (date.toEpochDay() - LocalDate.parse(meta.startDate).toEpochDay());
        double trendScaled = Math.min((double) timeIndex / meta.timeIndexMax, 1.25);

        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        int dow = date.getDayOfWeek().getValue() - 1; // Python pandas: Monday = 0, Sunday = 6

        double isWeekend = (dow == 5 || dow == 6) ? 1.0 : 0.0;
        double isPromo = isPromoDate(date) ? 1.0 : 0.0;

        return new float[]{
                clip(revenueScaledValue, 0.0f, 1.0f),
                (float) trendScaled,
                (float) Math.sin(2 * Math.PI * month / 12),
                (float) Math.cos(2 * Math.PI * month / 12),
                (float) Math.sin(2 * Math.PI * dow / 7),
                (float) Math.cos(2 * Math.PI * dow / 7),
                (float) Math.sin(2 * Math.PI * day / 31),
                (float) Math.cos(2 * Math.PI * day / 31),
                (float) isWeekend,
                (float) isPromo
        };
    }

    private boolean isPromoDate(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        return (month == 1 && day == 1)
                || (month == 6 && day == 6)
                || (month == 9 && day == 9)
                || (month == 11 && day == 11)
                || (month == 12 && day == 12);
    }

    private double inverseTransform(float scaledValue) {
        double clippedScaled = Math.max(0.0, Math.min(1.0, scaledValue));
        double logValue = clippedScaled * (meta.logMax - meta.logMin + EPS) + meta.logMin;
        double revenue = Math.expm1(logValue);

        if (revenue < 0) {
            revenue = 0;
        }

        if (revenue > meta.revenueCap) {
            revenue = meta.revenueCap;
        }

        return revenue;
    }

    private void slideWindow(float[][] window, float[] newRow) {
        for (int i = 0; i < meta.windowSize - 1; i++) {
            window[i] = window[i + 1];
        }
        window[meta.windowSize - 1] = newRow;
    }

    private float clip(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void destroy() throws Exception {
        if (session != null) {
            session.close();
        }
    }

    public record DailyForecastPoint(LocalDate date, double forecastRevenue) {
    }

    public static class RevenueLstmMeta {
        public String modelType;
        public String target;
        public int windowSize;
        public int nFeatures;
        public String inputName;
        public String outputName;
        public List<Integer> inputShape;
        public List<String> featureColumns;
        public String targetTransform;
        public double targetCapQuantile;
        public double revenueCap;
        public double logMin;
        public double logMax;
        public String startDate;
        public String lastTrainingDate;
        public int timeIndexMax;
        public int trainingRows;
        public List<List<Double>> lastWindowFeatures;
        public List<Double> lastWindowRevenue;
        public Metrics metrics;
        public ForecastPreview forecastPreview;
        public String note;
    }

    public static class Metrics {
        public double mae;
        public double rmse;
        public Double mape;
    }

    public static class ForecastPreview {
        public double forecast7Days;
        public double forecast30Days;
    }
}
