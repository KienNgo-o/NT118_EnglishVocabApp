package com.example.nt118_englishvocabapp.network.dto;
public class SignUpRequest {
    String username;
    String password;
    String email;

    public SignUpRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}