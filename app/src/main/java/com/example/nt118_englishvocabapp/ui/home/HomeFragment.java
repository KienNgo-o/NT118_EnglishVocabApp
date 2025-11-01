package com.example.nt118_englishvocabapp.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentHomeBinding;
import com.example.nt118_englishvocabapp.ui.account.AccountFragment;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private static final List<String> WEEK_DAYS = Arrays.asList("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su");

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Find views
        TextView quoteText = root.findViewById(R.id.textDialog);
        TextView activeDays = root.findViewById(R.id.text_active_days);
        TextView vocabProgress1 = root.findViewById(R.id.text_vocab_progress);
        TextView vocabProgress2 = root.findViewById(R.id.text_vocab_progress2);
        TextView quizProgress1 = root.findViewById(R.id.text_quiz_progress);
        TextView quizProgress2 = root.findViewById(R.id.text_quiz_progress2);
        TextView flashProgress = root.findViewById(R.id.text_flash_sub);
        ImageView avatar = root.findViewById(R.id.image_avatar);
        LinearLayout rowDays = root.findViewById(R.id.row_days);

        // Observe and populate
        homeViewModel.getTodayQuote().observe(getViewLifecycleOwner(), s -> {
            if (quoteText != null) quoteText.setText(s);
        });
        homeViewModel.getActiveDaysText().observe(getViewLifecycleOwner(), s -> {
            if (activeDays != null) activeDays.setText(s);
        });
        homeViewModel.getVocabProgress1().observe(getViewLifecycleOwner(), s -> {
            if (vocabProgress1 != null) vocabProgress1.setText(s);
        });
        homeViewModel.getVocabProgress2().observe(getViewLifecycleOwner(), s -> {
            if (vocabProgress2 != null) vocabProgress2.setText(s);
        });
        homeViewModel.getQuizProgress1().observe(getViewLifecycleOwner(), s -> {
            if (quizProgress1 != null) quizProgress1.setText(s);
        });
        homeViewModel.getQuizProgress2().observe(getViewLifecycleOwner(), s -> {
            if (quizProgress2 != null) quizProgress2.setText(s);
        });
        homeViewModel.getFlashProgress().observe(getViewLifecycleOwner(), s -> {
            if (flashProgress != null) flashProgress.setText(s);
        });

        // Observe study days and render the 7 day indicators
        homeViewModel.getStudyDays().observe(getViewLifecycleOwner(), activeList -> {
            if (rowDays == null) return;
            rowDays.removeAllViews();

            for (String day : WEEK_DAYS) {
                boolean isActive = activeList != null && activeList.contains(day);

                // create container
                LinearLayout containerDay = new LinearLayout(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd((int) (8 * requireContext().getResources().getDisplayMetrics().density));
                containerDay.setLayoutParams(lp);
                containerDay.setOrientation(LinearLayout.VERTICAL);
                containerDay.setGravity(android.view.Gravity.CENTER);

                // circle frame (rounded tile)
                FrameLayout circle = new FrameLayout(requireContext());
                int sizePx = (int) (48 * requireContext().getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams circleLp = new LinearLayout.LayoutParams(sizePx, sizePx);
                circle.setLayoutParams(circleLp);
                circle.setPadding(8, 8, 8, 8);

                // background rounded rect depends on active
                circle.setBackgroundResource(isActive ? R.drawable.rounded_rect_purple : R.drawable.rounded_rect_gray);

                // Centered check icon directly on the rounded rect
                ImageView check = new ImageView(requireContext());
                FrameLayout.LayoutParams checkLp = new FrameLayout.LayoutParams(
                        (int) (24 * requireContext().getResources().getDisplayMetrics().density),
                        (int) (24 * requireContext().getResources().getDisplayMetrics().density));
                checkLp.gravity = android.view.Gravity.CENTER;
                check.setLayoutParams(checkLp);
                check.setImageResource(R.drawable.ic_check);
                if (isActive) {
                    check.setColorFilter(Color.WHITE);
                    check.setAlpha(1f);
                } else {
                    check.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_gray));
                    check.setAlpha(0.35f);
                }
                circle.addView(check);

                // label under the tile (Mo, Tu...); always dark text per design
                TextView label = new TextView(requireContext());
                LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                label.setLayoutParams(labelLp);
                label.setText(day);
                label.setTextSize(12);
                label.setTextColor(Color.BLACK);
                label.setPadding(0, 6, 0, 0);

                containerDay.addView(circle);
                containerDay.addView(label);

                rowDays.addView(containerDay);
            }
        });

        // Avatar click -> open AccountFragment
        avatar.setOnClickListener(v -> {
            try {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.frame_layout, new AccountFragment())
                            .addToBackStack(null)
                            .commit();
                }
            } catch (Exception ignored) {
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}