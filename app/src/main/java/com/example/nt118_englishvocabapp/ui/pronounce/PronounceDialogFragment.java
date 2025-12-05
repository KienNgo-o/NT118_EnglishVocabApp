package com.example.nt118_englishvocabapp.ui.pronounce;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.nt118_englishvocabapp.R;

public class PronounceDialogFragment extends DialogFragment {

    public PronounceDialogFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Optional: allow dialog to be canceled by tapping outside
        setCancelable(true);
        // Use full-screen style so we can place a transparent background overlay and center a card
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the dialog layout (it uses a transparent parent with a centered card)
        View root = inflater.inflate(R.layout.dialog_pronounce, container, false);

        // Wire up start button to dismiss the dialog
        Button btnStart = root.findViewById(R.id.btn_start);
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> dismiss());
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make the dialog window transparent and occupy the full screen so the dim and overlay work
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // Increase dim amount so the screen behind the dialog is darker (0 = no dim, 1 = fully dark)
            getDialog().getWindow().setDimAmount(0.6f);
        }
    }
}
