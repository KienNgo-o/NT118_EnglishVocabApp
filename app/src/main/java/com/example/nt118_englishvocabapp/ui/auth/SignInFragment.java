package com.example.nt118_englishvocabapp.ui.auth;

// Thêm các import này
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

// Imports cho network
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.MainActivity;
import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;
import com.example.nt118_englishvocabapp.network.SessionManager;
import com.example.nt118_englishvocabapp.network.dto.SignInRequest;
import com.example.nt118_englishvocabapp.network.dto.SignInResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SignInFragment extends Fragment {

    // Khai báo views
    private EditText etUsername, etPassword;
    private Button btnSignIn;
    private ProgressBar progressBar;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        // Khởi tạo ApiService và SessionManager
        apiService = RetrofitClient.getApiService(getContext());
        sessionManager = SessionManager.getInstance(getContext());

        // Ánh xạ views
        etUsername = view.findViewById(R.id.input_username);
        etPassword = view.findViewById(R.id.input_password);
        btnSignIn = view.findViewById(R.id.button_sign_in_to_main);
        //progressBar = view.findViewById(R.id.progress_bar_sign_in); // Thêm ProgressBar vào layout
        //progressBar.setVisibility(View.GONE); // Ẩn đi lúc đầu

        // Nút đăng nhập
        btnSignIn.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Determine email input: prefer a dedicated input_email field if present, otherwise use username field
            String emailToSave = null;
            try {
                EditText etEmail = getView() != null ? getView().findViewById(R.id.input_email) : null;
                if (etEmail != null) {
                    emailToSave = etEmail.getText().toString().trim();
                }
            } catch (Exception ignored) {}
            if (emailToSave == null || emailToSave.isEmpty()) {
                // fallback: treat the username field as email if a dedicated email field is not present
                emailToSave = username;
            }

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập username và password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hiển thị ProgressBar, vô hiệu hoá nút
            showLoading(true);

            // Gọi API, pass emailToSave so we can persist exactly what the user entered as their email
            signInUser(username, password, emailToSave);
        });

        // Nút chuyển sang màn hình Sign up
        view.findViewById(R.id.button_go_to_sign_up).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new SignUpFragment())
                    .addToBackStack(null)
                    .commit();
            Toast.makeText(getContext(), "Ok!", Toast.LENGTH_SHORT).show();
        });

        // Nút chuyển sang màn hình Forgot password 1
        view.findViewById(R.id.button_forgot_password).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new ForgotPasswordFragment())
                    .addToBackStack(null)
                    .commit();
            Toast.makeText(getContext(), "Ok!", Toast.LENGTH_SHORT).show();
        });

        // Social placeholders
        ImageButton fb = view.findViewById(R.id.social_facebook_sign_in);
        ImageButton g = view.findViewById(R.id.social_google_sign_in);
        fb.setOnClickListener(v -> Toast.makeText(getContext(), "Facebook sign-in not implemented", Toast.LENGTH_SHORT).show());
        g.setOnClickListener(v -> Toast.makeText(getContext(), "Google sign-in not implemented", Toast.LENGTH_SHORT).show());


        return view;
    }

    private void signInUser(String username, String password, String emailToSave) {
        SignInRequest request = new SignInRequest(username, password);

        // Retrofit tự động chạy call này trên background thread
        // và trả kết quả về main thread qua onResponse/onFailure
        apiService.signIn(request).enqueue(new Callback<SignInResponse>() {
            @Override
            public void onResponse(@NonNull Call<SignInResponse> call, @NonNull Response<SignInResponse> response) {
                showLoading(false); // Ẩn loading

                if (response.isSuccessful() && response.body() != null) {
                    // Đăng nhập THÀNH CÔNG
                    SignInResponse signInResponse = response.body();

                    // Lưu token
                    sessionManager.saveAuthTokens(
                            signInResponse.getAccessToken(),
                            signInResponse.getRefreshToken()
                    );

                    // Save the username so other screens can display it
                    try {
                        sessionManager.saveUsername(username);
                        // Save the email exactly as user inserted (emailToSave)
                        sessionManager.saveEmail(emailToSave);
                    } catch (Exception ignored) {}

                    // Chuyển sang MainActivity
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                } else {
                    // Đăng nhập THẤT BẠI (ví dụ: 401 - Sai pass)
                    Toast.makeText(getContext(), "Username hoặc password không chính xác", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SignInResponse> call, @NonNull Throwable t) {
                showLoading(false); // Ẩn loading

                // Lỗi mạng hoặc server sập
                Log.e("SignInError", "onFailure: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
//            progressBar.setVisibility(View.VISIBLE);
            btnSignIn.setEnabled(false);
        } else {
      //      progressBar.setVisibility(View.GONE);
            btnSignIn.setEnabled(true);
        }
    }
}