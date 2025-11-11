package com.example.nt118_englishvocabapp.ui.auth;// Thêm các import này
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

// Imports cho network
import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;
import com.example.nt118_englishvocabapp.network.SignUpRequest;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpFragment extends Fragment {

    // Khai báo views
    private EditText etUsername, etEmail, etPassword;
    private Button btnSignUp;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false); // Thay R.layout

        // Khởi tạo ApiService
        apiService = RetrofitClient.getApiService(getContext());

        // Ánh xạ views
        etUsername = view.findViewById(R.id.input_username);
        etEmail = view.findViewById(R.id.input_email);
        etPassword = view.findViewById(R.id.input_password);
        btnSignUp = view.findViewById(R.id.button_sign_up);

        // Nút đăng ký
        btnSignUp.setOnClickListener(v -> handleSignUp());

        // Nút chuyển sang màn hình Sign in
        view.findViewById(R.id.button_go_to_sign_in).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new SignInFragment())
                    .addToBackStack(null)
                    .commit();
            Toast.makeText(getContext(), "Ok!", Toast.LENGTH_SHORT).show();
        });

        ImageButton fb = view.findViewById(R.id.social_facebook_sign_up);
        ImageButton g = view.findViewById(R.id.social_google_sign_up);
        fb.setOnClickListener(v -> Toast.makeText(getContext(), "Facebook sign-up not implemented", Toast.LENGTH_SHORT).show());
        g.setOnClickListener(v -> Toast.makeText(getContext(), "Google sign-up not implemented", Toast.LENGTH_SHORT).show());
        return view;
    }

    private void handleSignUp() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        // 1. Kiểm tra (Validate) input
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            return;
        }

        if (password.length() < 6) { // Thêm điều kiện nếu bạn muốn
            etPassword.setError("Password phải có ít nhất 6 ký tự");
            return;
        }

        // 2. Hiển thị loading
        showLoading(true);

        // 3. Tạo request
        SignUpRequest request = new SignUpRequest(username, password, email);

        // 4. Gọi API
        apiService.signUp(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                showLoading(false);

                if (response.isSuccessful()) {
                    // Thành công (Code 204)
                    Log.i("SignUp", "Đăng ký thành công!");
                    // Quay về Sign In
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container_view_auth, new SignInFragment())
                            .addToBackStack(null)
                            .commit();
                    Toast.makeText(getContext(), "Đã đăng ký thành công! Quay lại Sign In", Toast.LENGTH_LONG).show();
                } else {
                    // Thất bại
                    if (response.code() == 409) {
                        // 409: Conflict (Trùng username)
                        Toast.makeText(getContext(), "Username hoặc email đã tồn tại", Toast.LENGTH_LONG).show();
                    } else {
                        // Các lỗi khác (ví dụ 400, 500)
                        Toast.makeText(getContext(), "Đăng ký thất bại. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                showLoading(false);
                // Lỗi mạng hoặc server sập
                Log.e("SignUpError", "onFailure: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            btnSignUp.setEnabled(false);
        } else {
            btnSignUp.setEnabled(true);
        }
    }
}
