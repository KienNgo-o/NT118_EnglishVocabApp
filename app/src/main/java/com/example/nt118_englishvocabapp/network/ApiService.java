package com.example.nt118_englishvocabapp.network;

import com.example.nt118_englishvocabapp.models.RefreshRequest;
import com.example.nt118_englishvocabapp.models.RefreshResponse;
import com.example.nt118_englishvocabapp.models.SignOutRequest;
import com.example.nt118_englishvocabapp.models.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/api/auth/signin")
    Call<SignInResponse> signIn(@Body SignInRequest request);

    @POST("/api/auth/refresh")
    Call<RefreshResponse> refresh(@Body RefreshRequest request);

    @POST("/api/auth/signout")
    Call<Void> signOut(@Body SignOutRequest request); // <Void> vì server chỉ trả về 204 No Content

//    @GET("/api/users/me")
//    Call<User> getMe(); // Giả sử bạn có 1 lớp User.java
}