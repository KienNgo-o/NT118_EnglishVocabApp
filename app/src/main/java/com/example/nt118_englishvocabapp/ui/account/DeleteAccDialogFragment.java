// java
package com.example.nt118_englishvocabapp.ui.account;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class DeleteAccDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = requireActivity().getLayoutInflater()
                .inflate(com.example.nt118_englishvocabapp.R.layout.dialog_deleteacc, null);
        dialog.setContentView(content);

        View ivClose = content.findViewById(com.example.nt118_englishvocabapp.R.id.ivClose);
        View btnConfirm = content.findViewById(com.example.nt118_englishvocabapp.R.id.btnConfirm);

        if (ivClose != null) ivClose.setOnClickListener(v -> dismiss());
        if (btnConfirm != null) btnConfirm.setOnClickListener(v -> dismiss());

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
