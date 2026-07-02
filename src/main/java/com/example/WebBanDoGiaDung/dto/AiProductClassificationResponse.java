package com.example.WebBanDoGiaDung.dto;

public class AiProductClassificationResponse {
    private boolean success;
    private Integer categoryId;
    private String categoryName;
    private Integer brandId;
    private String brandName;
    private double confidence;
    private boolean lowConfidence;
    private String provider;
    private String message;

    public AiProductClassificationResponse() {
    }

    public static AiProductClassificationResponse failure(String message) {
        AiProductClassificationResponse response = new AiProductClassificationResponse();
        response.setSuccess(false);
        response.setConfidence(0.0);
        response.setLowConfidence(true);
        response.setMessage(message);
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getBrandId() {
        return brandId;
    }

    public void setBrandId(Integer brandId) {
        this.brandId = brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isLowConfidence() {
        return lowConfidence;
    }

    public void setLowConfidence(boolean lowConfidence) {
        this.lowConfidence = lowConfidence;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
