package com.example.nt118_englishvocabapp.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;

public class SignUpFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        // Nút quay lại màn hình đăng nhập
        view.findViewById(R.id.button_sign_up_back_to_sign_in).setOnClickListener(v -> {
            // Quay lại fragment trước đó trong back stack
            getParentFragmentManager().popBackStack();
        });

        return view;
    }
}
