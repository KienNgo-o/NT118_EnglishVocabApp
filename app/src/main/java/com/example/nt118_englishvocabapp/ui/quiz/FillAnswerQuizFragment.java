package com.example.nt118_englishvocabapp.ui.quiz;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

public class FillAnswerQuizFragment extends Fragment {

    private View root;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_quiz_fillanswer, container, false);

        // Keyboard listener (same as before)
        if (getActivity() != null) {
            keyboardRootView = requireActivity().findViewById(android.R.id.content);
        } else {
            keyboardRootView = root;
        }

        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean lastStateVisible = false;
            @Override
            public void onGlobalLayout() {
                if (keyboardRootView == null || getActivity() == null) return;
                Rect r = new Rect();
                keyboardRootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = keyboardRootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;
                if (isKeyboardVisible == lastStateVisible) return;
                lastStateVisible = isKeyboardVisible;
                View bottomAppBar = requireActivity().findViewById(R.id.bottomAppBar);
                View fab = requireActivity().findViewById(R.id.fab);
                if (bottomAppBar != null) bottomAppBar.setVisibility(isKeyboardVisible ? View.GONE : View.VISIBLE);
                if (fab != null) fab.setVisibility(isKeyboardVisible ? View.GONE : View.VISIBLE);
            }
        };

        if (keyboardRootView != null) {
            keyboardRootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        }

        // preClick hides keyboard before performing the return action
        View.OnClickListener preClick = v -> {
            if (!isAdded()) return;
            KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);
            keyboardListener = null;
        };

        Runnable fallback = () -> {
            if (!isAdded()) return;
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new HomeFragment())
                    .commitAllowingStateLoss();
        };

        ReturnButtonHelper.bind(root, this, preClick, fallback);

        // UI wiring
        TextView txtIndex = root.findViewById(R.id.txt_question_index);
        TextView txtQuestion = root.findViewById(R.id.txt_question);
        TextView txtPartial = root.findViewById(R.id.txt_partial_word);

        View indicator1 = root.findViewById(R.id.indicator_1);
        View indicator2 = root.findViewById(R.id.indicator_2);
        View indicator3 = root.findViewById(R.id.indicator_3);

        EditText edtAnswer = root.findViewById(R.id.edt_answer);
        Button btnConfirm = root.findViewById(R.id.btn_confirm);

        // Example data: full answer from resources (existing)
        final String fullAnswer = getString(R.string.answer_a).trim(); // e.g. "yellow"

        // Try to get partial template resource named partial_answer_a (optional)
        int partialResId = getResources().getIdentifier("partial_answer_a", "string", requireContext().getPackageName());
        final String partialTemplate = (partialResId != 0) ? getString(partialResId).trim() : generatePartialTemplate(fullAnswer);

        // Show texts
        txtIndex.setText(R.string.question_1_of_3);
        txtQuestion.setText("Fill in the blank:");
        txtPartial.setText(partialTemplate);

        indicator1.setSelected(true);
        indicator2.setSelected(false);
        indicator3.setSelected(false);

        // Confirm click: user types only the missing substring(s)
        btnConfirm.setOnClickListener(v -> {
            if (!isAdded()) return;
            KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);
            keyboardListener = null;

            String input = edtAnswer.getText() == null ? "" : edtAnswer.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the missing part", Toast.LENGTH_SHORT).show();
                return;
            }

            // compute expected missing substring(s) by comparing fullAnswer and partialTemplate
            String expectedMissing = extractMissingFrom(fullAnswer, partialTemplate);

            // disable inputs
            edtAnswer.setEnabled(false);
            btnConfirm.setEnabled(false);

            if (expectedMissing.equalsIgnoreCase(input)) {
                edtAnswer.setBackgroundResource(R.drawable.rounded_green);
                edtAnswer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                Toast.makeText(requireContext(), "Correct!", Toast.LENGTH_SHORT).show();
            } else {
                edtAnswer.setBackgroundResource(R.drawable.rounded_red);
                edtAnswer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                Toast.makeText(requireContext(), "Incorrect. Answer: " + fullAnswer, Toast.LENGTH_LONG).show();
            }
        });

        return root;
    }

    private String extractMissingFrom(String full, String partial) {
        if (full == null) return "";
        if (partial == null) return "";
        StringBuilder sb = new StringBuilder();
        int min = Math.min(full.length(), partial.length());
        for (int i = 0; i < min; i++) {
            char pc = partial.charAt(i);
            if (pc == '_' || pc == '�') { // underscore or placeholder
                sb.append(full.charAt(i));
            }
        }
        // if partial shorter than full and partial ends with underscore-like intent
        if (partial.length() < full.length()) {
            for (int i = partial.length(); i < full.length(); i++) {
                // assume masked tail as missing
                sb.append(full.charAt(i));
            }
        }
        return sb.toString();
    }

    private String generatePartialTemplate(String full) {
        if (full == null || full.length() == 0) return "";
        int len = full.length();
        // mask a small middle portion (at least 1 char)
        int start = Math.max(1, len / 3);
        int maskLen = Math.max(1, Math.min(len - start - 1, len / 5));
        StringBuilder sb = new StringBuilder(full);
        for (int i = start; i < start + maskLen && i < len; i++) {
            sb.setCharAt(i, '_');
        }
        return sb.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (keyboardRootView != null && keyboardListener != null) {
            try {
                keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
            } catch (Exception ignored) {}
        }
        keyboardRootView = null;
        keyboardListener = null;
        root = null;
    }
}
