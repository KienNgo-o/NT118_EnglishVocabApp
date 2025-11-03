// Tự động thêm Authorization: Bearer <token> vào header
package com.example.nt118_englishvocabapp;

import android.content.Context;
import androidx.annotation.NonNull;

import com.example.nt118_englishvocabapp.network.SessionManager;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        sessionManager = SessionManager.getInstance(context);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Không thêm token vào các request (signin, signup, refresh)
        if (originalRequest.url().encodedPath().contains("/api/auth/signin") ||
                originalRequest.url().encodedPath().contains("/api/auth/refresh")) {
            return chain.proceed(originalRequest);
        }

        String accessToken = sessionManager.getAccessToken();
        if (accessToken == null) {
            return chain.proceed(originalRequest); // Không có token, cứ gửi request
        }

        // Thêm token vào header
        Request newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();

        return chain.proceed(newRequest);
    }
}