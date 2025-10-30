package com.example.nt118_englishvocabapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.MainActivity;
import com.example.nt118_englishvocabapp.R;

public class SignUpFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        // Continue -> go to MainActivity for now (no backend yet)
        view.findViewById(R.id.button_sign_up_continue).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Nút quay lại màn hình đăng nhập
        view.findViewById(R.id.button_sign_up_back_to_sign_in).setOnClickListener(v -> {
            // Quay lại fragment trước đó trong back stack
            getParentFragmentManager().popBackStack();
        });

        ImageButton fb = view.findViewById(R.id.social_facebook);
        ImageButton g = view.findViewById(R.id.social_google);
        fb.setOnClickListener(v -> Toast.makeText(getContext(), "Facebook sign-up not implemented", Toast.LENGTH_SHORT).show());
        g.setOnClickListener(v -> Toast.makeText(getContext(), "Google sign-up not implemented", Toast.LENGTH_SHORT).show());

        return view;
    }
}
