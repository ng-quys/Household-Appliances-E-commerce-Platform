package com.example.WebBanDoGiaDung.service;

public class MomoPaymentException extends RuntimeException {
    private final String resultCode;
    private final String localMessage;
    private final String orderId;
    private final String requestId;
    private final String responseBody;
    private final String momoOrderId;
    private final String momoTransactionId;

    public MomoPaymentException(String message,
                                String resultCode,
                                String localMessage,
                                String orderId,
                                String requestId,
                                String responseBody,
                                String momoOrderId,
                                String momoTransactionId) {
        super(message);
        this.resultCode = resultCode;
        this.localMessage = localMessage;
        this.orderId = orderId;
        this.requestId = requestId;
        this.responseBody = responseBody;
        this.momoOrderId = momoOrderId;
        this.momoTransactionId = momoTransactionId;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getLocalMessage() {
        return localMessage;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getMomoOrderId() {
        return momoOrderId;
    }

    public String getMomoTransactionId() {
        return momoTransactionId;
    }
}
