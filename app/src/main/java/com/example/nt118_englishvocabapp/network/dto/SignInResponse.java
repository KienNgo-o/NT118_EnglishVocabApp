package com.example.nt118_englishvocabapp.network.dto;

public class SignInResponse {
    private String accessToken;
    private String refreshToken;

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
}