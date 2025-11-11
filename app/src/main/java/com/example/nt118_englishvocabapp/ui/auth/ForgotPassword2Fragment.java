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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.nt118_englishvocabapp.LoginActivity;
import com.example.nt118_englishvocabapp.R;

public class ForgotPassword2Fragment extends Fragment {

    public ForgotPassword2Fragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_forgot_password2, container, false);

        EditText newPass = v.findViewById(R.id.input_new_password);
        EditText confirm = v.findViewById(R.id.input_confirm_password);
        Button cont = v.findViewById(R.id.button_continue_fp2);

        cont.setOnClickListener(view -> {
            String n = newPass.getText().toString();
            String c = confirm.getText().toString();
            if (TextUtils.isEmpty(n)) {
                newPass.setError("Enter new password");
                return;
            }
            if (!n.equals(c)) {
                confirm.setError("Passwords do not match");
                return;
            }

            CongratulationDialogFragment dlg = new CongratulationDialogFragment();
            dlg.setListener(() -> {
                // Để LoginActivity xử lý việc quay về màn hình Sign In
                if (getActivity() instanceof LoginActivity) {
                    ((LoginActivity) getActivity()).returnToSignInScreen();
                }
            });
            dlg.show(getParentFragmentManager(), "congrats");
        });
        return v;
    }
}
