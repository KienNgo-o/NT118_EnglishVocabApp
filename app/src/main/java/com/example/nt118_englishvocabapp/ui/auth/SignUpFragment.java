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

        // Continue -> quay lại Sign In (no backend yet)
        view.findViewById(R.id.button_sign_up).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new SignInFragment())
                    .addToBackStack(null)
                    .commit();
            Toast.makeText(getContext(), "Account Registration Successful!", Toast.LENGTH_SHORT).show();
        });

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
}
