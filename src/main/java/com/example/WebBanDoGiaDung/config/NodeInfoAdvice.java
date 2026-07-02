package com.example.WebBanDoGiaDung.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NodeInfoAdvice {

    @Value("${server.port}")
    private String serverPort;

    @ModelAttribute("nodePort")
    public String nodePort() {
        return serverPort;
    }
}