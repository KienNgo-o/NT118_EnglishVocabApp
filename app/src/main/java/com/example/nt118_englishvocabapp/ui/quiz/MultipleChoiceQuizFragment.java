package com.example.nt118_englishvocabapp.ui.quiz;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

/**
 * Simple multiple-choice fragment that uses the `fragment_quiz_multiplechoice.xml` layout.
 * - Header/return behavior is wired using ReturnButtonHelper (same as other fragments)
 * - Shows question index, three indicators, question text and four answer buttons
 * - Handles answer selection: highlights correct/incorrect and disables further input
 */
public class MultipleChoiceQuizFragment extends Fragment {

    private View root;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_quiz_multiplechoice, container, false);

        // Keyboard visibility listener (same approach as existing QuizFragment)
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

        // Return button binding: hide keyboard then navigate back (or home fallback)
        View.OnClickListener preClick = v -> {
            if (!isAdded()) return;
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);
        };
        Runnable fallback = () -> {
            if (!isAdded()) return;
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new HomeFragment())
                    .commitAllowingStateLoss();
        };
        ReturnButtonHelper.bind(root, this, preClick, fallback);

        // Wire views
        TextView txtIndex = root.findViewById(R.id.txt_question_index);
        TextView txtQuestion = root.findViewById(R.id.txt_question);
        View indicator1 = root.findViewById(R.id.indicator_1);
        View indicator2 = root.findViewById(R.id.indicator_2);
        View indicator3 = root.findViewById(R.id.indicator_3);

        Button btnA = root.findViewById(R.id.btn_answer_a);
        Button btnB = root.findViewById(R.id.btn_answer_b);
        Button btnC = root.findViewById(R.id.btn_answer_c);
        Button btnD = root.findViewById(R.id.btn_answer_d);

        // Example: set question/index from resources (could be replaced by ViewModel/data later)
        txtIndex.setText(R.string.question_1_of_3);
        txtQuestion.setText(R.string.question_example);

        // Example: mark first indicator active (tint via background selector is used by drawable)
        indicator1.setSelected(true);
        indicator2.setSelected(false);
        indicator3.setSelected(false);

        // Answer handling: assume answer A is correct for this example
        View.OnClickListener answerClick = v -> {
            // disable all buttons after selection
            btnA.setEnabled(false);
            btnB.setEnabled(false);
            btnC.setEnabled(false);
            btnD.setEnabled(false);

            Button selected = (Button) v;
            // Simple correctness check: btnA is correct
            boolean isCorrect = selected.getId() == R.id.btn_answer_a;

            if (isCorrect) {
                // green background for correct
                int green = ContextCompat.getColor(requireContext(), R.color.correct_green);
                selected.setBackgroundColor(green);
                selected.setTextColor(Color.WHITE);
            } else {
                // red for wrong, highlight correct one as well
                int red = ContextCompat.getColor(requireContext(), R.color.incorrect_red);
                selected.setBackgroundColor(red);
                selected.setTextColor(Color.WHITE);
                int green = ContextCompat.getColor(requireContext(), R.color.correct_green);
                btnA.setBackgroundColor(green);
                btnA.setTextColor(Color.WHITE);
            }
        };

        btnA.setOnClickListener(answerClick);
        btnB.setOnClickListener(answerClick);
        btnC.setOnClickListener(answerClick);
        btnD.setOnClickListener(answerClick);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (keyboardRootView != null && keyboardListener != null) {
            keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
        keyboardRootView = null;
        keyboardListener = null;
    }
}
