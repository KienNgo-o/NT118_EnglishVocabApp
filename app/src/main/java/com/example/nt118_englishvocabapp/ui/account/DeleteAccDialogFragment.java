package com.example.nt118_englishvocabapp.ui.account;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class DeleteAccDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = requireActivity().getLayoutInflater()
                .inflate(com.example.nt118_englishvocabapp.R.layout.dialog_deleteacc, null, false);
        dialog.setContentView(content);

        View ivClose = content.findViewById(com.example.nt118_englishvocabapp.R.id.ivClose);
        Button btnConfirm = content.findViewById(com.example.nt118_englishvocabapp.R.id.btnConfirm);
        EditText etPassword = content.findViewById(com.example.nt118_englishvocabapp.R.id.et_delete_password);

        if (ivClose != null) ivClose.setOnClickListener(v -> dismiss());

        // Disable nút confirm cho đến khi user nhập password
        if (btnConfirm != null) btnConfirm.setEnabled(false);

        if (etPassword != null) {
            etPassword.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (btnConfirm != null) {
                        btnConfirm.setEnabled(s != null && !s.toString().trim().isEmpty());
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) { }
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String pass = null;
                if (etPassword != null && etPassword.getText() != null) {
                    pass = etPassword.getText().toString();
                }

                // Gửi password được nhập vào caller sử dụng Fragment Result API
                Bundle result = new Bundle();
                result.putString("password", pass);
                getParentFragmentManager().setFragmentResult("delete_account_confirm", result);

                dismiss();
            });
        }

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
