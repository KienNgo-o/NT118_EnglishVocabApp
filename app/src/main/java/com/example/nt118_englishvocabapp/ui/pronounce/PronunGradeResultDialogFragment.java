package com.example.nt118_englishvocabapp.ui.pronounce;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class PronunGradeResultDialogFragment extends DialogFragment {
    private static final String ARG_SCORE = "arg_score";
    private static final String ARG_FEEDBACK = "arg_feedback";

    public static PronunGradeResultDialogFragment newInstance(double score, String feedback) {
        PronunGradeResultDialogFragment f = new PronunGradeResultDialogFragment();
        Bundle b = new Bundle();
        b.putDouble(ARG_SCORE, score);
        b.putString(ARG_FEEDBACK, feedback);
        f.setArguments(b);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        double score = 0.0;
        String feedback = null;
        if (args != null) {
            score = args.getDouble(ARG_SCORE, 0.0);
            feedback = args.getString(ARG_FEEDBACK, "");
        }

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setTitle("Pronunciation Result");
        String message = String.format(java.util.Locale.getDefault(), "Score: %.1f\n\nFeedback: %s", score, (feedback == null || feedback.isEmpty()) ? "(no feedback)" : feedback);
        b.setMessage(message);
        b.setPositiveButton(android.R.string.ok, (d, which) -> dismiss());
        return b.create();
    }
}

