package com.example.nt118_englishvocabapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.MainActivity;
import com.example.nt118_englishvocabapp.R;

public class SignInFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        // Nút đăng nhập vào màn MainActivity
        view.findViewById(R.id.button_sign_in_to_main).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            // Cờ này sẽ xóa hết các Activity cũ và tạo Task mới cho MainActivity
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Nút chuyển sang màn hình Sign up
        view.findViewById(R.id.button_go_to_sign_up).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new SignUpFragment())
                    .addToBackStack(null) // Thêm vào back stack để có thể quay lại
                    .commit();
        });

        // Nút chuyển sang màn hình Forgot password 1
        view.findViewById(R.id.button_forgot_password).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new ForgotPasswordFragment())
                    .addToBackStack(null) // Thêm vào back stack để có thể quay lại
                    .commit();
        });


        return view;
    }
}
