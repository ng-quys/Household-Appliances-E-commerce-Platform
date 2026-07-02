package com.example.WebBanDoGiaDung.service.ai;

import com.example.WebBanDoGiaDung.dto.AiProductClassificationResponse;
import com.example.WebBanDoGiaDung.entity.Brand;
import com.example.WebBanDoGiaDung.entity.Genre;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiProductClassificationService {
    private static final long MAX_IMAGE_SIZE = 5L * 1024L * 1024L;
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}");

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${ai.provider:gemini}")
    private String provider;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${ai.openai-model:gpt-4.1-mini}")
    private String openAiModel;

    @Value("${ai.gemini-model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${ai.confidence-threshold:0.7}")
    private double confidenceThreshold;

    public AiProductClassificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiProductClassificationResponse classify(MultipartFile imageFile, List<Genre> genres, List<Brand> brands) {
        if (imageFile == null || imageFile.isEmpty()) {
            return AiProductClassificationResponse.failure("Vui lòng chọn ảnh sản phẩm để AI phân loại.");
        }
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            return AiProductClassificationResponse.failure("Ảnh vượt quá 5MB. Vui lòng chọn ảnh nhỏ hơn.");
        }
        if (genres == null || genres.isEmpty()) {
            return AiProductClassificationResponse.failure("Chưa có danh mục/thể loại trong hệ thống để AI chọn.");
        }
        if (brands == null || brands.isEmpty()) {
            return AiProductClassificationResponse.failure("Chưa có hãng sản xuất trong hệ thống để AI chọn.");
        }

        String normalizedProvider = normalizeProvider(provider);
        String apiKey = resolveApiKey(normalizedProvider);
        if (apiKey == null || apiKey.isBlank()) {
            return AiProductClassificationResponse.failure("Chưa cấu hình API key cho AI. Hãy cấu hình AI_API_KEY, OPENAI_API_KEY hoặc GEMINI_API_KEY ở backend.");
        }

        try {
            String mimeType = normalizeMimeType(imageFile.getContentType());
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            String prompt = buildPrompt(genres, brands);
            String aiText = "openai".equals(normalizedProvider)
                    ? callOpenAi(apiKey, prompt, base64Image, mimeType)
                    : callGemini(apiKey, prompt, base64Image, mimeType);

            AiProductClassificationResponse parsed = parseAiResponse(aiText);
            parsed.setProvider(normalizedProvider);
            return validateAndNormalize(parsed, genres, brands);
        } catch (RestClientResponseException exception) {
            return AiProductClassificationResponse.failure("AI trả lỗi HTTP " + exception.getStatusCode().value() + ". Vui lòng kiểm tra API key, model hoặc hạn mức tài khoản.");
        } catch (Exception exception) {
            return AiProductClassificationResponse.failure("Không thể phân loại ảnh bằng AI: " + exception.getMessage());
        }
    }

    private String callOpenAi(String apiKey, String prompt, String base64Image, String mimeType) {
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        Map<String, Object> imageUrl = new LinkedHashMap<>();
        imageUrl.put("url", dataUrl);

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        content.add(Map.of("type", "image_url", "image_url", imageUrl));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", content);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", openAiModel);
        body.put("temperature", 0);
        body.put("max_tokens", 450);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(message));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<?, ?> responseBody = response.getBody();
        List<?> choices = getList(responseBody, "choices");
        if (choices.isEmpty()) {
            throw new IllegalStateException("OpenAI không trả về lựa chọn phân loại.");
        }
        Map<?, ?> firstChoice = asMap(choices.get(0));
        Map<?, ?> messageMap = asMap(firstChoice.get("message"));
        Object contentText = messageMap.get("content");
        if (contentText == null || contentText.toString().isBlank()) {
            throw new IllegalStateException("OpenAI không trả về nội dung phân loại.");
        }
        return contentText.toString();
    }

    private String callGemini(String apiKey, String prompt, String base64Image, String mimeType) {
        Map<String, Object> inlineData = new LinkedHashMap<>();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64Image);

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        parts.add(Map.of("inline_data", inlineData));

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", parts);

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel
                + ":generateContent?key="
                + apiKey;

        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(body, headers), Map.class);
        Map<?, ?> responseBody = response.getBody();
        List<?> candidates = getList(responseBody, "candidates");
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Gemini không trả về kết quả phân loại.");
        }
        Map<?, ?> firstCandidate = asMap(candidates.get(0));
        Map<?, ?> contentMap = asMap(firstCandidate.get("content"));
        List<?> responseParts = getList(contentMap, "parts");
        if (responseParts.isEmpty()) {
            throw new IllegalStateException("Gemini không trả về nội dung phân loại.");
        }
        Map<?, ?> firstPart = asMap(responseParts.get(0));
        Object text = firstPart.get("text");
        if (text == null || text.toString().isBlank()) {
            throw new IllegalStateException("Gemini không trả về nội dung phân loại.");
        }
        return text.toString();
    }

    private String buildPrompt(List<Genre> genres, List<Brand> brands) throws IOException {
        List<Map<String, Object>> genreItems = genres.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Genre::getGenreId, Comparator.nullsLast(Integer::compareTo)))
                .map(genre -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", genre.getGenreId());
                    item.put("name", genre.getGenreName());
                    return item;
                })
                .toList();

        List<Map<String, Object>> brandItems = brands.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Brand::getBrandId, Comparator.nullsLast(Integer::compareTo)))
                .map(brand -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", brand.getBrandId());
                    item.put("name", brand.getBrandName());
                    return item;
                })
                .toList();

        String genreJson = objectMapper.writeValueAsString(genreItems);
        String brandJson = objectMapper.writeValueAsString(brandItems);

        return """
                Bạn là hệ thống AI phân loại ảnh sản phẩm cho website bán đồ gia dụng.
                Nhiệm vụ: xem ảnh sản phẩm và chọn đúng 1 thể loại, đúng 1 hãng sản xuất trong danh sách có sẵn.

                Quy tắc bắt buộc:
                - Chỉ được chọn id/name có trong danh sách bên dưới.
                - Không tự tạo thêm thể loại hoặc hãng mới.
                - Nếu không thấy rõ logo/hãng thì brandId và brandName để null.
                - Nếu không chắc thể loại thì categoryId và categoryName để null.
                - confidence là số từ 0 đến 1, phản ánh độ chắc chắn tổng thể.
                - Chỉ trả về một JSON object hợp lệ, không markdown, không giải thích ngoài JSON.

                Danh sách thể loại hiện có:
                %s

                Danh sách hãng sản xuất hiện có:
                %s

                JSON bắt buộc đúng dạng:
                {
                  "categoryId": 1,
                  "categoryName": "Tên thể loại hoặc null",
                  "brandId": 1,
                  "brandName": "Tên hãng hoặc null",
                  "confidence": 0.86,
                  "message": "AI đã tự phân loại sản phẩm"
                }
                """.formatted(genreJson, brandJson);
    }

    private AiProductClassificationResponse parseAiResponse(String aiText) throws IOException {
        String json = extractJsonObject(aiText);
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

        AiProductClassificationResponse response = new AiProductClassificationResponse();
        response.setSuccess(true);
        response.setCategoryId(toInteger(map.get("categoryId")));
        response.setCategoryName(toCleanString(map.get("categoryName")));
        response.setBrandId(toInteger(map.get("brandId")));
        response.setBrandName(toCleanString(map.get("brandName")));
        response.setConfidence(clampConfidence(toDouble(map.get("confidence"))));
        response.setMessage(toCleanString(map.get("message")));
        return response;
    }

    private AiProductClassificationResponse validateAndNormalize(AiProductClassificationResponse response, List<Genre> genres, List<Brand> brands) {
        Optional<Genre> genre = findGenre(response.getCategoryId(), response.getCategoryName(), genres);
        Optional<Brand> brand = findBrand(response.getBrandId(), response.getBrandName(), brands);

        response.setCategoryId(genre.map(Genre::getGenreId).orElse(null));
        response.setCategoryName(genre.map(Genre::getGenreName).orElse(null));
        response.setBrandId(brand.map(Brand::getBrandId).orElse(null));
        response.setBrandName(brand.map(Brand::getBrandName).orElse(null));

        boolean lowConfidence = response.getConfidence() < confidenceThreshold
                || response.getCategoryId() == null
                || response.getBrandId() == null;
        response.setLowConfidence(lowConfidence);
        if (lowConfidence) {
            response.setMessage("AI không chắc kết quả, vui lòng kiểm tra lại thể loại và hãng sản xuất.");
        } else if (response.getMessage() == null || response.getMessage().isBlank()) {
            response.setMessage("AI đã tự phân loại sản phẩm.");
        }
        return response;
    }

    private Optional<Genre> findGenre(Integer id, String name, List<Genre> genres) {
        if (id != null) {
            Optional<Genre> byId = genres.stream()
                    .filter(genre -> genre.getGenreId() != null && genre.getGenreId().equals(id))
                    .findFirst();
            if (byId.isPresent()) {
                return byId;
            }
        }
        String normalizedName = normalizeText(name);
        if (!normalizedName.isBlank()) {
            return genres.stream()
                    .filter(genre -> normalizeText(genre.getGenreName()).equals(normalizedName))
                    .findFirst();
        }
        return Optional.empty();
    }

    private Optional<Brand> findBrand(Integer id, String name, List<Brand> brands) {
        if (id != null) {
            Optional<Brand> byId = brands.stream()
                    .filter(brand -> brand.getBrandId() != null && brand.getBrandId().equals(id))
                    .findFirst();
            if (byId.isPresent()) {
                return byId;
            }
        }
        String normalizedName = normalizeText(name);
        if (!normalizedName.isBlank()) {
            return brands.stream()
                    .filter(brand -> normalizeText(brand.getBrandName()).equals(normalizedName))
                    .findFirst();
        }
        return Optional.empty();
    }

    private String normalizeProvider(String providerValue) {
        String value = providerValue == null ? "" : providerValue.trim().toLowerCase(Locale.ROOT);
        return "openai".equals(value) ? "openai" : "gemini";
    }

    private String resolveApiKey(String normalizedProvider) {
        if (aiApiKey != null && !aiApiKey.isBlank()) {
            return aiApiKey.trim();
        }
        if ("openai".equals(normalizedProvider) && openAiApiKey != null && !openAiApiKey.isBlank()) {
            return openAiApiKey.trim();
        }
        if ("gemini".equals(normalizedProvider) && geminiApiKey != null && !geminiApiKey.isBlank()) {
            return geminiApiKey.trim();
        }
        return "";
    }

    private String normalizeMimeType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "image/jpeg";
        }
        String value = contentType.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "image/png", "image/jpeg", "image/jpg", "image/webp" -> "image/jpg".equals(value) ? "image/jpeg" : value;
            default -> "image/jpeg";
        };
    }

    private String extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("AI trả về rỗng.");
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group();
        }
        throw new IllegalStateException("AI không trả về JSON hợp lệ.");
    }

    private List<?> getList(Map<?, ?> map, String key) {
        if (map == null) {
            return List.of();
        }
        Object value = map.get(key);
        return value instanceof List<?> list ? list : List.of();
    }

    private Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value.toString().trim();
        if (text.isBlank() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private double clampConfidence(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String toCleanString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isBlank() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return text;
    }

    private String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}
