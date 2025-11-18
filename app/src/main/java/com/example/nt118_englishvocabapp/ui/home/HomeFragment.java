package com.example.nt118_englishvocabapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.GridLayout;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentHomeBinding;
import com.example.nt118_englishvocabapp.ui.account.AccountFragment;
import com.example.nt118_englishvocabapp.util.StreakManager;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private StreakManager streakManager;
    private int displayYear;
    private int displayMonth; // zero-based

    private enum DayState { ACTIVE, FREEZE, INACTIVE }

    // no manual content-centering: ConstraintLayout positions the icon between date bottom and pill bottom
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        try {
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            streakManager = new StreakManager(requireContext());

            // Find views
            TextView quoteText = root.findViewById(R.id.textDialog);
            TextView activeDays = root.findViewById(R.id.text_active_days);
            TextView vocabProgress1 = root.findViewById(R.id.text_vocab_progress);
            TextView vocabProgress2 = root.findViewById(R.id.text_vocab_progress2);
            TextView quizProgress1 = root.findViewById(R.id.text_quiz_progress);
            TextView quizProgress2 = root.findViewById(R.id.text_quiz_progress2);
            TextView flashProgress = root.findViewById(R.id.text_flash_progress);
            ImageView avatar = root.findViewById(R.id.image_avatar);
            TextView greetingBig = root.findViewById(R.id.text_greeting_big);

            ImageButton prevMonth = root.findViewById(R.id.button_prev_month);
            ImageButton nextMonth = root.findViewById(R.id.button_next_month);
            TextView monthLabel = root.findViewById(R.id.text_month_label);
            GridLayout calendarGrid = root.findViewById(R.id.calendar_grid);

            // init display month to current
            Calendar c = Calendar.getInstance();
            displayYear = c.get(Calendar.YEAR);
            displayMonth = c.get(Calendar.MONTH);

            // Populate greeting: use account name from resources as fallback
            try {
                String accountName = getString(R.string.account_name);
                if (greetingBig != null) {
                    greetingBig.setText(getString(R.string.greeting_format, accountName));
                }
            } catch (Exception ignored) {
            }

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

            // month navigation
            prevMonth.setOnClickListener(v -> {
                displayMonth--;
                if (displayMonth < 0) {
                    displayMonth = 11;
                    displayYear--;
                }
                populateCalendar(calendarGrid, monthLabel, activeDays);
            });
            nextMonth.setOnClickListener(v -> {
                displayMonth++;
                if (displayMonth > 11) {
                    displayMonth = 0;
                    displayYear++;
                }
                populateCalendar(calendarGrid, monthLabel, activeDays);
            });

            // initial population
            populateCalendar(calendarGrid, monthLabel, activeDays);

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
        } catch (Exception ex) {
            Log.e("HomeFragment", "onCreateView error", ex);
            try {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Home UI error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception ignored) {}
            // return a simple empty view to avoid crashing
            return new View(requireContext());
        }
    }

    private void populateCalendar(GridLayout grid, TextView monthLabel, TextView activeDaysText) {
        if (grid == null || monthLabel == null) return;
        try {
            grid.removeAllViews();
        } catch (Exception ex) {
            Log.e("HomeFragment", "populateCalendar failed", ex);
            try {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Calendar error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception ignored) {}
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, displayYear);
        cal.set(Calendar.MONTH, displayMonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstWeekday = cal.get(Calendar.DAY_OF_WEEK); // Sunday=1 ... Saturday=7
        // convert to Monday=0..Sunday=6 index
        int startOffset = (firstWeekday + 5) % 7; // if Sunday(1)->6, Monday(2)->0, etc.

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // show month label
        String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        monthLabel.setText(monthName);

        // build a set of active days
        List<Integer> active = streakManager.getActiveDaysForMonth(displayYear, displayMonth);
        Set<Integer> activeSet = new HashSet<>();
        if (active != null && !active.isEmpty()) activeSet.addAll(active);

        // grid columns = 7. We'll create 6*7 cells (some empty)
        int totalCells = 6 * 7;
        for (int i = 0; i < totalCells; i++) {
            View cell = getLayoutInflater().inflate(R.layout.item_calendar_day, grid, false);
            TextView tv = cell.findViewById(R.id.text_day_number);
            ImageView flame = cell.findViewById(R.id.bg_flame);
            View cellRoot = cell.findViewById(R.id.day_cell_root);

            // Defensive: if the layout doesn't contain expected views, skip this cell
            if (tv == null || flame == null) {
                continue;
            }

            int dayNumber = i - startOffset + 1;
            if (dayNumber >= 1 && dayNumber <= daysInMonth) {
                tv.setText(String.valueOf(dayNumber));
                // Determine day state
                boolean isActive = activeSet.contains(dayNumber);
                Calendar today = Calendar.getInstance();
                int tYear = today.get(Calendar.YEAR);
                int tMonth = today.get(Calendar.MONTH);
                int tDay = today.get(Calendar.DAY_OF_MONTH);
                boolean isToday = (tYear == displayYear && tMonth == displayMonth && tDay == dayNumber);

                DayState state;
                if (isToday) {
                    // today can be ACTIVE (if marked) or FREEZE (if not marked)
                    state = isActive ? DayState.ACTIVE : DayState.FREEZE;
                } else {
                    state = isActive ? DayState.ACTIVE : DayState.INACTIVE;
                }

                // Apply background and text color based on state
                if (cellRoot != null) {
                    switch (state) {
                        case ACTIVE:
                            cellRoot.setBackgroundResource(R.drawable.day_peach_pill);
                            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.day_peach_text));
                            break;
                        case FREEZE:
                            cellRoot.setBackgroundResource(R.drawable.day_blue_pill);
                            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.day_blue_text));
                            break;
                        default:
                            cellRoot.setBackgroundResource(R.drawable.day_gray_pill);
                            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.day_gray_text));
                            break;
                    }
                }

                final int resId = (state == DayState.ACTIVE) ? R.drawable.streak : (state == DayState.FREEZE ? R.drawable.streak_freeze : R.drawable.streak_no);
                flame.setVisibility(View.VISIBLE);
                flame.setImageResource(resId);
                // Let layout handle vertical centering between date and pill bottom
                if (isActive && isToday) {
                    flame.post(() -> { try { animateToday(flame, cellRoot); } catch (Exception ignored){} });
                }
             } else {
                 // empty cell
                 tv.setText("");
                 flame.setVisibility(View.GONE);
                 if (cellRoot != null) cellRoot.setBackground(null);
             }

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            cell.setLayoutParams(lp);
            grid.addView(cell);
        }

        // update active days label
        int totalActive = streakManager.getTotalActiveDays();
        if (activeDaysText != null) {
            activeDaysText.setText(getResources().getQuantityString(R.plurals.active_days_count, totalActive, totalActive));
        }

        // update streak number on header
        try {
            // update streak number on header
            TextView headerStreak = null;
            View root = grid.getRootView();
            if (root != null) headerStreak = root.findViewById(R.id.text_streak_number);

            if (headerStreak == null && getActivity() != null) {
                headerStreak = getActivity().findViewById(R.id.text_streak_number);
            }
            if (headerStreak != null) {
                headerStreak.setText(String.valueOf(totalActive));
            }
        } catch (Exception ex) {
            Log.e("HomeFragment", "populateCalendar failed", ex);
            try {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Calendar error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception ignored) {}
        }
    }

    // flashy animation for today's active day: pop + pulse + slight container bounce
    private void animateToday(View icon, View container) {
        // Animate the icon from the center of the screen (oversized) back to its cell position.
        try {
            // Ensure measurements are available
            icon.post(() -> {
                try {
                    // current translation target (the icon may already have some translation due to layout)
                    float targetTX = icon.getTranslationX();
                    float targetTY = icon.getTranslationY();

                    // compute icon center on screen
                    int[] loc = new int[2];
                    icon.getLocationOnScreen(loc);
                    float iconCenterX = loc[0] + icon.getWidth() / 2f;
                    float iconCenterY = loc[1] + icon.getHeight() / 2f;

                    // screen center
                    final android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                    float screenCenterX = dm.widthPixels / 2f;
                    float screenCenterY = dm.heightPixels / 2f;

                    // compute starting translation so the icon appears at screen center
                    float startTX = screenCenterX - iconCenterX + targetTX;
                    float startTY = screenCenterY - iconCenterY + targetTY;

                    // set initial (start) state: positioned at center, large and invisible
                    icon.setTranslationX(startTX);
                    icon.setTranslationY(startTY);
                    icon.setScaleX(3.0f);
                    icon.setScaleY(3.0f);
                    icon.setAlpha(0f);

                    // animate translationX/Y, scale, and alpha to their target values
                    ObjectAnimator aTX = ObjectAnimator.ofFloat(icon, "translationX", startTX, targetTX);
                    ObjectAnimator aTY = ObjectAnimator.ofFloat(icon, "translationY", startTY, targetTY);
                    ObjectAnimator sX = ObjectAnimator.ofFloat(icon, "scaleX", 3.0f, 1.0f);
                    ObjectAnimator sY = ObjectAnimator.ofFloat(icon, "scaleY", 3.0f, 1.0f);
                    ObjectAnimator aAlpha = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f);

                    AnimatorSet arrive = new AnimatorSet();
                    arrive.playTogether(aTX, aTY, sX, sY, aAlpha);
                    arrive.setInterpolator(new OvershootInterpolator(1.6f));
                    arrive.setDuration(700);

                    // small pulse on the icon after arrival
                    ObjectAnimator pulseX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.08f);
                    ObjectAnimator pulseY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.08f);
                    pulseX.setRepeatMode(ValueAnimator.REVERSE);
                    pulseY.setRepeatMode(ValueAnimator.REVERSE);
                    pulseX.setRepeatCount(1);
                    pulseY.setRepeatCount(1);
                    pulseX.setDuration(220);
                    pulseY.setDuration(220);
                    AnimatorSet pulse = new AnimatorSet();
                    pulse.playTogether(pulseX, pulseY);

                    // slight container bounce for the pill
                    ObjectAnimator cScaleX = ObjectAnimator.ofFloat(container, "scaleX", 1f, 1.03f);
                    ObjectAnimator cScaleY = ObjectAnimator.ofFloat(container, "scaleY", 1f, 1.03f);
                    cScaleX.setRepeatMode(ValueAnimator.REVERSE);
                    cScaleY.setRepeatMode(ValueAnimator.REVERSE);
                    cScaleX.setRepeatCount(1);
                    cScaleY.setRepeatCount(1);
                    cScaleX.setDuration(220);
                    cScaleY.setDuration(220);
                    AnimatorSet containerPulse = new AnimatorSet();
                    containerPulse.playTogether(cScaleX, cScaleY);

                    AnimatorSet seq = new AnimatorSet();
                    seq.playSequentially(arrive, pulse, containerPulse);
                    seq.start();
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
