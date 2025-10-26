package com.example.nt118_englishvocabapp.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.nt118_englishvocabapp.R;

public class CongratulationDialogFragment extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_congratulation, container, false);
        setCancelable(false); // Ngăn người dùng đóng dialog

        view.findViewById(R.id.button_return_to_sign_in).setOnClickListener(v -> {
            // 1. Đóng dialog
            dismiss();

            // 2. Xóa hết các fragment trong back stack để quay về màn hình đầu tiên (SignIn)
            getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // 3. Thay thế container bằng một SignInFragment mới
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new SignInFragment())
                    .commit();
        });

        return view;
    }
}
