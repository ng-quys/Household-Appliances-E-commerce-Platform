package com.example.WebBanDoGiaDung.service;

import java.math.BigDecimal;
import java.util.Map;

public interface MomoService {
    String createPayment(BigDecimal amount, Integer orderId);

    boolean verifyIpnSignature(Map<String, String> payload);

    Integer resolveInternalOrderId(String momoOrderId, String extraData);
}
