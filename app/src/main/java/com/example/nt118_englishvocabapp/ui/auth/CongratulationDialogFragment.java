package com.example.nt118_englishvocabapp.ui.auth;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.nt118_englishvocabapp.R;

public class CongratulationDialogFragment extends DialogFragment {

    public interface CongratsListener { void onReturnToSignIn(); }
    private CongratsListener listener;
    public void setListener(CongratsListener l) { listener = l; }

    // flag to request navigation after dialog is dismissed
    private boolean navigateAfterDismiss = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_congratulation, container, false);
        Button btn = v.findViewById(R.id.button_return_sign_in);
        btn.setOnClickListener(view -> {
            // mark navigation and dismiss; actual navigation happens in onDismiss
            navigateAfterDismiss = true;
            dismiss();
        });
        return v;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (navigateAfterDismiss && listener != null) {
            listener.onReturnToSignIn();
        }
        navigateAfterDismiss = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
