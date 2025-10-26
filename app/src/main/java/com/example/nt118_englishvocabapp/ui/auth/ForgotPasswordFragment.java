package com.example.nt118_englishvocabapp.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;

public class ForgotPasswordFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        // Nút hiển thị Dialog OTP
        view.findViewById(R.id.button_show_otp).setOnClickListener(v -> {
            OtpDialogFragment otpDialog = new OtpDialogFragment();
            otpDialog.show(getParentFragmentManager(), "OtpDialogFragment");
        });

        return view;
    }
}
