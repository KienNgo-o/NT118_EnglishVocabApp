package com.example.nt118_englishvocabapp.ui.pronounce;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class PronunProgressDialogFragment extends DialogFragment {
    private static final String ARG_MESSAGE = "arg_message";

    public static PronunProgressDialogFragment newInstance(String message) {
        PronunProgressDialogFragment f = new PronunProgressDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_MESSAGE, message);
        f.setArguments(b);
        f.setCancelable(false);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String message = args != null ? args.getString(ARG_MESSAGE, "Working…") : "Working…";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(com.example.nt118_englishvocabapp.R.layout.dialog_pronun_progress, null);
        TextView tv = v.findViewById(com.example.nt118_englishvocabapp.R.id.progress_message);
        tv.setText(message);
        builder.setView(v);
        builder.setCancelable(false);
        return builder.create();
    }
}
