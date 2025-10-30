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

        // Keyboard visibility listener setup (same pattern used elsewhere)
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
            keyboardListener = null; // helper returned null; ensure local reference cleared
        };

        Runnable fallback = () -> {
            if (!isAdded()) return;
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new HomeFragment())
                    .commitAllowingStateLoss();
        };

        ReturnButtonHelper.bind(root, this, preClick, fallback);

        // Wire UI elements
        TextView txtIndex = root.findViewById(R.id.txt_question_index);
        TextView txtQuestion = root.findViewById(R.id.txt_question);
        View indicator1 = root.findViewById(R.id.indicator_1);
        View indicator2 = root.findViewById(R.id.indicator_2);
        View indicator3 = root.findViewById(R.id.indicator_3);

        EditText edtAnswer = root.findViewById(R.id.edt_answer);
        Button btnConfirm = root.findViewById(R.id.btn_confirm);

        // Example sample data - this can be replaced with ViewModel/data later
        txtIndex.setText(R.string.question_1_of_3);
        txtQuestion.setText(R.string.question_example);

        // Mark first indicator active
        indicator1.setSelected(true);
        indicator2.setSelected(false);
        indicator3.setSelected(false);

        // Determine expected answer (use one of the string resources for example)
        final String expected = getString(R.string.answer_a).trim().toLowerCase();

        btnConfirm.setOnClickListener(v -> {
            if (!isAdded()) return;

            // Hide keyboard and restore UI
            KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);
            keyboardListener = null;

            String input = edtAnswer.getText() == null ? "" : edtAnswer.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an answer", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable inputs after confirming
            edtAnswer.setEnabled(false);
            btnConfirm.setEnabled(false);

            String lower = input.toLowerCase();
            if (lower.equals(expected)) {
                // correct
                int green = ContextCompat.getColor(requireContext(), R.color.correct_green);
                edtAnswer.setBackgroundColor(green);
                edtAnswer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                Toast.makeText(requireContext(), "Correct!", Toast.LENGTH_SHORT).show();
            } else {
                // incorrect
                int red = ContextCompat.getColor(requireContext(), R.color.incorrect_red);
                edtAnswer.setBackgroundColor(red);
                edtAnswer.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                Toast.makeText(requireContext(), "Incorrect. Answer: " + getString(R.string.answer_a), Toast.LENGTH_LONG).show();
            }
        });

        return root;
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
