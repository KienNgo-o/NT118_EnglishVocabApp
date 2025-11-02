package com.example.nt118_englishvocabapp.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.nt118_englishvocabapp.R;

public class ForgotPasswordFragment extends Fragment {

    public ForgotPasswordFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        EditText email = v.findViewById(R.id.input_email_fp);
        Button continueBtn = v.findViewById(R.id.button_continue_fp);

        continueBtn.setOnClickListener(view -> {
            String e = email.getText().toString().trim();
            if (TextUtils.isEmpty(e)) {
                email.setError("Enter email");
                return;
            }
            // simulate sending OTP -> show OTP dialog
            OtpDialogFragment otp = new OtpDialogFragment();
            otp.setListener(() -> {
                // open second fragment to enter new password
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(((ViewGroup)requireActivity().findViewById(android.R.id.content)).getId(),
                                new ForgotPassword2Fragment())
                        .addToBackStack(null)
                        .commit();
            });
            otp.show(getParentFragmentManager(), "otp");
        });

        return v;
    }
}
