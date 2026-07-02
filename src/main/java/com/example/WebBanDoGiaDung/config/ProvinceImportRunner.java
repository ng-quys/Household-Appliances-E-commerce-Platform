//package com.example.WebBanDoGiaDung.config;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.Data;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.List;
//
//@Component
//@Profile("import-address")
//public class ProvinceImportRunner implements CommandLineRunner {
//
//    private final JdbcTemplate jdbcTemplate;
//    private final ObjectMapper objectMapper;
//
//    public ProvinceImportRunner(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
//        this.jdbcTemplate = jdbcTemplate;
//        this.objectMapper = objectMapper;
//    }
//
//
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("=== ProvinceImportRunner dang chay ===");
//
//        String apiUrl = "https://provinces.open-api.vn/api/v1/?depth=3";
//
//        HttpClient client = HttpClient.newBuilder()
//                .connectTimeout(java.time.Duration.ofSeconds(30))
//                .build();
//
//        System.out.println("Dang goi API tinh thanh...");
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(apiUrl))
//                .timeout(java.time.Duration.ofSeconds(120))
//                .GET()
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        System.out.println("API status = " + response.statusCode());
//        System.out.println("API body length = " + response.body().length());
//
//        ProvinceDto[] provinces = objectMapper.readValue(response.body(), ProvinceDto[].class);
//
//        int districtCount = 0;
//        int wardCount = 0;
//
//        for (ProvinceDto province : provinces) {
//            if (province.getDistricts() != null) {
//                districtCount += province.getDistricts().size();
//
//                for (DistrictDto district : province.getDistricts()) {
//                    if (district.getWards() != null) {
//                        wardCount += district.getWards().size();
//                    }
//                }
//            }
//        }
//
//        System.out.println("API provinces = " + provinces.length);
//        System.out.println("API districts = " + districtCount);
//        System.out.println("API wards = " + wardCount);
//
//        for (ProvinceDto province : provinces) {
//            System.out.println("Import province: " + province.getName());
//
//            insertProvince(province);
//
//            if (province.getDistricts() == null) continue;
//
//            for (DistrictDto district : province.getDistricts()) {
//                insertDistrict(district, province.getCode());
//
//                if (district.getWards() == null) continue;
//
//                for (WardDto ward : district.getWards()) {
//                    insertWard(ward, district.getCode());
//                }
//            }
//        }
//
//        System.out.println("Import tỉnh/huyện/xã xong. Tổng tỉnh/thành: " + provinces.length);
//    }
//
//    private void insertProvince(ProvinceDto province) {
//        jdbcTemplate.update("""
//                INSERT INTO provinces (province_id, province_name, type)
//                VALUES (?, ?, ?)
//                ON DUPLICATE KEY UPDATE
//                    province_name = VALUES(province_name),
//                    type = VALUES(type)
//                """,
//                province.getCode(),
//                province.getName(),
//                normalizeType(province.getDivisionType())
//        );
//    }
//
//    private void insertDistrict(DistrictDto district, Integer provinceId) {
//        jdbcTemplate.update("""
//                INSERT INTO districts (district_id, district_name, type, province_id)
//                VALUES (?, ?, ?, ?)
//                ON DUPLICATE KEY UPDATE
//                    district_name = VALUES(district_name),
//                    type = VALUES(type),
//                    province_id = VALUES(province_id)
//                """,
//                district.getCode(),
//                district.getName(),
//                normalizeType(district.getDivisionType()),
//                provinceId
//        );
//    }
//
//    private void insertWard(WardDto ward, Integer districtId) {
//        jdbcTemplate.update("""
//                INSERT INTO wards (ward_id, ward_name, type, district_id)
//                VALUES (?, ?, ?, ?)
//                ON DUPLICATE KEY UPDATE
//                    ward_name = VALUES(ward_name),
//                    type = VALUES(type),
//                    district_id = VALUES(district_id)
//                """,
//                ward.getCode(),
//                ward.getName(),
//                normalizeType(ward.getDivisionType()),
//                districtId
//        );
//    }
//
//    private String normalizeType(String value) {
//        if (value == null || value.isBlank()) {
//            return "";
//        }
//
//        String lower = value.toLowerCase();
//
//        if (lower.contains("thành phố")) return "Thành Phố Trung Ương";
//        if (lower.contains("tỉnh")) return "Tỉnh";
//        if (lower.contains("quận")) return "Quận";
//        if (lower.contains("huyện")) return "Huyện";
//        if (lower.contains("thị xã")) return "Thị Xã";
//        if (lower.contains("phường")) return "Phường";
//        if (lower.contains("xã")) return "Xã";
//        if (lower.contains("thị trấn")) return "Thị Trấn";
//
//        return value;
//    }
//
//    @Data
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    public static class ProvinceDto {
//        private String name;
//        private Integer code;
//        private String division_type;
//        private List<DistrictDto> districts;
//
//        public String getDivisionType() {
//            return division_type;
//        }
//    }
//
//    @Data
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    public static class DistrictDto {
//        private String name;
//        private Integer code;
//        private String division_type;
//        private List<WardDto> wards;
//
//        public String getDivisionType() {
//            return division_type;
//        }
//    }
//
//    @Data
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    public static class WardDto {
//        private String name;
//        private Integer code;
//        private String division_type;
//
//        public String getDivisionType() {
//            return division_type;
//        }
//    }
//}

//--spring.profiles.active=import-address