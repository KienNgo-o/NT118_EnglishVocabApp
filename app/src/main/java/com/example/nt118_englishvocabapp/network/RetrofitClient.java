package com.example.nt118_englishvocabapp.network;

import android.content.Context;

import com.example.nt118_englishvocabapp.AuthAuthenticator;
import com.example.nt118_englishvocabapp.AuthInterceptor;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // THAY THẾ IP NÀY BẰNG IP CỦA BẠN
    private static final String BASE_URL = "http://10.0.199.168:5001/";

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            apiService = getClient(context).create(ApiService.class);
        }
        return apiService;
    }

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // 1. Lấy Context để dùng cho SessionManager
            Context appContext = context.getApplicationContext();

            // 2. Tạo Logging Interceptor (để xem log request/response)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); // Log mọi thứ

            // 3. Cấu hình OkHttpClient
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging); // Thêm logging

            // 4. Thêm Interceptor để tự động chèn AccessToken
            httpClient.addInterceptor(new AuthInterceptor(appContext));

            // 5. Thêm Authenticator để tự động Refresh Token khi gặp lỗi 401
            httpClient.authenticator(new AuthAuthenticator(appContext));

            OkHttpClient client = httpClient.build();

            // 6. Tạo Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}