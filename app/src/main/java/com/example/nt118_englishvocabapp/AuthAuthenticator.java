package com.example.nt118_englishvocabapp;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nt118_englishvocabapp.network.SessionManager;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.dto.RefreshRequest;
import com.example.nt118_englishvocabapp.network.dto.RefreshResponse;
import com.example.nt118_englishvocabapp.network.RetrofitClient;

import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

public class AuthAuthenticator implements Authenticator {

    private Context context;
    private SessionManager sessionManager;

    public AuthAuthenticator(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = SessionManager.getInstance(context);
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        String refreshToken = sessionManager.getRefreshToken();

        // Nếu không có refresh token, hoặc đã thử refresh rồi (tránh lặp vô hạn)
        if (refreshToken == null || response.request().header("Authorization") != null) {
            // Không thể làm mới token -> Đăng xuất
            logoutUser();
            return null; // Dừng lại
        }

        // Gọi API /refresh ĐỒNG BỘ (BẮT BUỘC)
        // Lưu ý: Cần tạo 1 instance Retrofit "sạch" không dùng Authenticator
        // để tránh vòng lặp vô hạn khi gọi refresh.
        ApiService cleanApiService = RetrofitClient.getClient(context).create(ApiService.class);
        Call<RefreshResponse> call = cleanApiService.refresh(new RefreshRequest(refreshToken));

        try {
            retrofit2.Response<RefreshResponse> refreshResponse = call.execute(); // Chạy đồng bộ

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                String newAccessToken = refreshResponse.body().getAccessToken();

                // Lưu accessToken mới
                sessionManager.saveAuthTokens(newAccessToken, refreshToken); // Lưu lại token mới

                // Trả về request ban đầu với header Authorization đã được cập nhật
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newAccessToken)
                        .build();
            } else {
                // Refresh token thất bại (hết hạn hoặc không hợp lệ)
                logoutUser();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Lỗi mạng, dừng lại
        }
    }

    private void logoutUser() {
        sessionManager.clearTokens();
        // Chuyển về màn hình Login
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}