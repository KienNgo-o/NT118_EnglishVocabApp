package com.example.nt118_englishvocabapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentHomeBinding;
import com.example.nt118_englishvocabapp.ui.account.AccountFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

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
        TextView flashProgress = root.findViewById(R.id.text_flash_progress);
        ImageView avatar = root.findViewById(R.id.image_avatar);
        LinearLayout rowDays = root.findViewById(R.id.row_days);
        TextView greetingBig = root.findViewById(R.id.text_greeting_big);

        // Ensure the study-days row is on top of other views (avoid accidental overlay)
        if (rowDays != null) {
            try {
                rowDays.bringToFront();
                rowDays.setTranslationZ(20f);
            } catch (Exception ignored) {
            }
        }

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

        // Observe study days and update the 7 day ImageButtons in the layout
        homeViewModel.getStudyDays().observe(getViewLifecycleOwner(), activeList -> {
            if (rowDays == null) return; // layout not ready

            // Find the ImageButtons defined in fragment_home.xml
            ImageButton mon = root.findViewById(R.id.day_mon);
            ImageButton tue = root.findViewById(R.id.day_tue);
            ImageButton wed = root.findViewById(R.id.day_wed);
            ImageButton thu = root.findViewById(R.id.day_thu);
            ImageButton fri = root.findViewById(R.id.day_fri);
            ImageButton sat = root.findViewById(R.id.day_sat);
            ImageButton sun = root.findViewById(R.id.day_sun);

            List<ImageButton> buttons = Arrays.asList(mon, tue, wed, thu, fri, sat, sun);
            List<String> keys = Arrays.asList("mo", "tu", "we", "th", "fr", "sa", "su");

            // Normalize backend strings to short keys (mo, tu, ...)
            Set<String> normalized = new HashSet<>();
            if (activeList != null) {
                for (String s : activeList) {
                    if (s == null) continue;
                    String n = s.toLowerCase(Locale.ROOT).trim();
                    // handle common formats: Mon, Mon., Monday, mo, etc.
                    if (n.startsWith("mo")) normalized.add("mo");
                    else if (n.startsWith("tu")) normalized.add("tu");
                    else if (n.startsWith("we")) normalized.add("we");
                    else if (n.startsWith("th")) normalized.add("th");
                    else if (n.startsWith("fr")) normalized.add("fr");
                    else if (n.startsWith("sa")) normalized.add("sa");
                    else if (n.startsWith("su")) normalized.add("su");
                }
            }

            // Update each button's image depending on whether it's active
            for (int i = 0; i < buttons.size(); i++) {
                ImageButton b = buttons.get(i);
                String key = keys.get(i);
                if (b == null) continue;
                if (normalized.contains(key)) {
                    b.setImageResource(R.drawable.daycheck_yes);
                    b.setAlpha(1f);
                } else {
                    b.setImageResource(R.drawable.daycheck_no);
                    b.setAlpha(0.95f);
                }
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