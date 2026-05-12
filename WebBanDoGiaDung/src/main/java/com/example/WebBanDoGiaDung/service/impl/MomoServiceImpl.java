package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.service.MomoPaymentException;
import com.example.WebBanDoGiaDung.service.MomoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MomoServiceImpl implements MomoService {

    private static final Logger log = LoggerFactory.getLogger(MomoServiceImpl.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.redirect-url}")
    private String redirectUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    @Value("${momo.request-type:captureWallet}")
    private String requestType;

    @Override
    public String createPayment(BigDecimal amount, Integer orderId) {
        validateConfig();

        BigDecimal normalizedAmount = amount.setScale(0, RoundingMode.HALF_UP);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MomoPaymentException(
                    "Số tiền thanh toán không hợp lệ.",
                    "LOCAL_INVALID_AMOUNT",
                    null,
                    String.valueOf(orderId),
                    null,
                    null,
                    null,
                    null
            );
        }

        String internalOrderId = String.valueOf(orderId);
        String momoOrderId = buildMomoOrderId(orderId);
        String requestId = momoOrderId;
        String orderInfo = "Thanh toan don hang #" + orderId;
        String extraData = "internalOrderId=" + internalOrderId;
        String amountText = normalizedAmount.toPlainString();

        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + amountText
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + momoOrderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        String signature = hmacSha256(rawSignature, secretKey);
        Map<String, String> requestBody = new LinkedHashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "WebBanDoGiaDung");
        requestBody.put("storeId", "WebBanDoGiaDung");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amountText);
        requestBody.put("orderId", momoOrderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", redirectUrl);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);
        String body = toJson(requestBody);

        log.info("MoMo create request: endpoint={}, partnerCode={}, requestId={}, amount={}, momoOrderId={}, internalOrderId={}, orderInfo={}, redirectUrl={}, ipnUrl={}, requestType={}, extraData={}, lang=vi",
                endpoint, partnerCode, requestId, amountText, momoOrderId, internalOrderId, orderInfo, redirectUrl, ipnUrl, requestType, extraData);
        log.info("MoMo raw signature: {}", rawSignature);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseBody = response.body();
            String payUrl = extractJsonValue(responseBody, "payUrl");
            String resultCode = extractJsonValue(responseBody, "resultCode");
            String message = extractJsonValue(responseBody, "message");
            String localMessage = extractJsonValue(responseBody, "localMessage");
            String responseOrderId = extractJsonValue(responseBody, "orderId");
            String responseRequestId = extractJsonValue(responseBody, "requestId");
            String transId = extractJsonValue(responseBody, "transId");

            log.info("MoMo create response: resultCode={}, message={}, localMessage={}, momoOrderId={}, requestId={}, transId={}, body={}",
                    resultCode, message, localMessage, responseOrderId, responseRequestId, transId, sanitizeResponseBody(responseBody));

            if (payUrl == null || payUrl.isBlank()) {
                throw new MomoPaymentException(
                        buildMomoErrorMessage(resultCode, message),
                        resultCode,
                        localMessage,
                        internalOrderId,
                        responseRequestId != null ? responseRequestId : requestId,
                        sanitizeResponseBody(responseBody),
                        responseOrderId != null ? responseOrderId : momoOrderId,
                        transId
                );
            }
            return payUrl;
        } catch (MomoPaymentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MomoPaymentException(
                    "Không thể gọi MoMo API: " + exception.getMessage(),
                    "LOCAL_HTTP_ERROR",
                    null,
                    internalOrderId,
                    requestId,
                    null,
                    momoOrderId,
                    null
            );
        }
    }

    @Override
    public boolean verifyIpnSignature(Map<String, String> payload) {
        String signature = payload.get("signature");
        if (signature == null || signature.isBlank()) {
            return false;
        }

        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("accessKey", accessKey);
        ordered.put("amount", payload.getOrDefault("amount", ""));
        ordered.put("extraData", payload.getOrDefault("extraData", ""));
        ordered.put("message", payload.getOrDefault("message", ""));
        ordered.put("orderId", payload.getOrDefault("orderId", ""));
        ordered.put("orderInfo", payload.getOrDefault("orderInfo", ""));
        ordered.put("orderType", payload.getOrDefault("orderType", ""));
        ordered.put("partnerCode", payload.getOrDefault("partnerCode", ""));
        ordered.put("payType", payload.getOrDefault("payType", ""));
        ordered.put("requestId", payload.getOrDefault("requestId", ""));
        ordered.put("responseTime", payload.getOrDefault("responseTime", ""));
        ordered.put("resultCode", payload.getOrDefault("resultCode", ""));
        ordered.put("transId", payload.getOrDefault("transId", ""));

        String rawSignature = ordered.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");

        return signature.equals(hmacSha256(rawSignature, secretKey));
    }

    public Integer resolveInternalOrderId(String momoOrderId, String extraData) {
        if (extraData != null && extraData.startsWith("internalOrderId=")) {
            return safeParseInteger(extraData.substring("internalOrderId=".length()));
        }
        if (momoOrderId != null && momoOrderId.startsWith("ORDER-")) {
            String[] parts = momoOrderId.split("-");
            if (parts.length >= 3) {
                return safeParseInteger(parts[1]);
            }
        }
        return safeParseInteger(momoOrderId);
    }

    private String buildMomoOrderId(Integer orderId) {
        return "ORDER-" + orderId + "-" + System.currentTimeMillis();
    }

    private Integer safeParseInteger(String value) {
        try {
            return value != null ? Integer.valueOf(value.trim()) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void validateConfig() {
        if (isBlank(endpoint) || isBlank(partnerCode) || isBlank(accessKey) || isBlank(secretKey)
                || isBlank(redirectUrl) || isBlank(ipnUrl) || isBlank(requestType)) {
            throw new MomoPaymentException(
                    "Thiếu cấu hình MoMo bắt buộc trong application.properties.",
                    "LOCAL_CONFIG_ERROR",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể ký HMAC SHA256", exception);
        }
    }

    private String toJson(Map<String, String> data) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escape(entry.getKey())).append('"')
                    .append(':')
                    .append('"').append(escape(entry.getValue())).append('"');
        }
        builder.append('}');
        return builder.toString();
    }

    private String extractJsonValue(String json, String field) {
        String token = "\"" + field + "\":";
        int start = json.indexOf(token);
        if (start < 0) {
            return null;
        }
        int valueStart = start + token.length();
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) {
            return null;
        }
        if (json.charAt(valueStart) == '"') {
            int stringStart = valueStart + 1;
            int stringEnd = json.indexOf('"', stringStart);
            return stringEnd >= 0 ? json.substring(stringStart, stringEnd) : null;
        }
        int valueEnd = valueStart;
        while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd).trim();
    }

    private String sanitizeResponseBody(String responseBody) {
        if (responseBody == null) {
            return null;
        }
        return responseBody.replaceAll("(?i)\"signature\"\\s*:\\s*\"[^\"]*\"", "\"signature\":\"***\"");
    }

    private String buildMomoErrorMessage(String resultCode, String message) {
        String safeCode = resultCode != null ? resultCode : "unknown";
        String safeMessage = message != null && !message.isBlank() ? message : "Không rõ lý do";
        return "MoMo không trả payUrl: resultCode=" + safeCode + ", message=" + safeMessage;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
