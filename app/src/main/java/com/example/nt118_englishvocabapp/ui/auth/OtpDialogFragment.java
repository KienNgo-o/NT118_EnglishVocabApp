package com.example.nt118_englishvocabapp.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.nt118_englishvocabapp.R;

public class OtpDialogFragment extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_otp, container, false);

        view.findViewById(R.id.button_confirm_otp).setOnClickListener(v -> {
            // 1. Đóng dialog này
            dismiss();

            // 2. Chuyển sang màn hình Forgot password 2
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new ForgotPassword2Fragment())
                    // không cần addToBackStack nếu bạn muốn khi back sẽ về thẳng Sign In
                    .commit();
        });

        return view;
    }
}
