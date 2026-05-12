package com.example.WebBanDoGiaDung.dto;

public class ChatResponse {
    private boolean success;
    private String reply;

    public ChatResponse(boolean success, String reply) {
        this.success = success;
        this.reply = reply;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReply() {
        return reply;
    }
}
