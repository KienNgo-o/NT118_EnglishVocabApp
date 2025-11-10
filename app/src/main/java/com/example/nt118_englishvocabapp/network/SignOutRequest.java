package com.example.nt118_englishvocabapp.network;

public class SignOutRequest {

    // Tên biến này (refreshToken) phải khớp chính xác
    // với key trong JSON mà backend mong đợi
    String refreshToken;

    public SignOutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}