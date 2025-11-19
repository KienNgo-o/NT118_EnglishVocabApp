package com.example.nt118_englishvocabapp.ui.quiz;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

public class MultipleChoiceQuizFragment extends Fragment {

    private View root;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_quiz_multiplechoice, container, false);

        // Keyboard visibility listener
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

        // Return button binding
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
        ImageView imgQuestion = root.findViewById(R.id.img_question);
        View indicator1 = root.findViewById(R.id.indicator_1);
        View indicator2 = root.findViewById(R.id.indicator_2);
        View indicator3 = root.findViewById(R.id.indicator_3);

        Button btnA = root.findViewById(R.id.btn_answer_a);
        Button btnB = root.findViewById(R.id.btn_answer_b);
        Button btnC = root.findViewById(R.id.btn_answer_c);
        Button btnD = root.findViewById(R.id.btn_answer_d);

        // Ensure default appearance for buttons (rounded white, remove theme tint)
        btnA.setBackgroundResource(R.drawable.rounded_white);
        btnB.setBackgroundResource(R.drawable.rounded_white);
        btnC.setBackgroundResource(R.drawable.rounded_white);
        btnD.setBackgroundResource(R.drawable.rounded_white);
        btnA.setBackgroundTintList(null);
        btnB.setBackgroundTintList(null);
        btnC.setBackgroundTintList(null);
        btnD.setBackgroundTintList(null);
        ViewCompat.setBackgroundTintList(btnA, null);
        ViewCompat.setBackgroundTintList(btnB, null);
        ViewCompat.setBackgroundTintList(btnC, null);
        ViewCompat.setBackgroundTintList(btnD, null);

        // Sample/default data (can be replaced by arguments or ViewModel)
        txtIndex.setText(R.string.question_1_of_3);

        // Load image from arguments or fallback drawable
        int imageRes = R.drawable.apple; // replace with your drawable
        if (getArguments() != null && getArguments().containsKey("image_res")) {
            imageRes = getArguments().getInt("image_res", imageRes);
        }
        // Enforce modest size and scaling
        imgQuestion.setAdjustViewBounds(true);
        imgQuestion.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int padDp = 8;
        int padPx = (int) (padDp * getResources().getDisplayMetrics().density + 0.5f);
        imgQuestion.setPadding(padPx, padPx, padPx, padPx);
        imgQuestion.setImageResource(imageRes);

        // Question related to image
        txtQuestion.setText("What color is this fruit?"); // replace with dynamic text if needed

        // Set choices appropriate to the question
        btnA.setText("Red");
        btnB.setText("Blue");
        btnC.setText("Green");
        btnD.setText("Yellow");

        indicator1.setSelected(true);
        indicator2.setSelected(false);
        indicator3.setSelected(false);

        // Answer handling: assume btnA ("Red") is correct
        View.OnClickListener answerClick = v -> {
            // prevent further clicks
            btnA.setClickable(false);
            btnB.setClickable(false);
            btnC.setClickable(false);
            btnD.setClickable(false);

            Button selected = (Button) v;
            int correctId = R.id.btn_answer_a;
            boolean isCorrect = selected.getId() == correctId;

            // reset visuals to deterministic base
            btnA.setBackgroundResource(R.drawable.rounded_white);
            btnB.setBackgroundResource(R.drawable.rounded_white);
            btnC.setBackgroundResource(R.drawable.rounded_white);
            btnD.setBackgroundResource(R.drawable.rounded_white);
            btnA.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
            btnB.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
            btnC.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
            btnD.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));

            if (isCorrect) {
                selected.setBackgroundResource(R.drawable.answer_button_bg_correct);
                selected.setTextColor(Color.WHITE);
            } else {
                selected.setBackgroundResource(R.drawable.answer_button_bg_incorrect);
                selected.setTextColor(Color.WHITE);

                View correctBtn = root.findViewById(correctId);
                if (correctBtn instanceof Button) {
                    Button c = (Button) correctBtn;
                    c.setBackgroundResource(R.drawable.answer_button_bg_correct);
                    c.setTextColor(Color.WHITE);
                }
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
