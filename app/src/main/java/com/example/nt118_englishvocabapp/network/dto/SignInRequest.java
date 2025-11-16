package com.example.nt118_englishvocabapp.network.dto;

public class SignInRequest {
    private String username;
    private String password;

    public SignInRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}