package com.example.nt118_englishvocabapp.ui.vocab;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentVocabBinding;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.ui.vocab2.VocabFragment2;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;
import android.util.Log;

public class VocabFragment extends Fragment {

    private static final String TAG = "VocabFragment";
    private FragmentVocabBinding binding;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentVocabBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Toast.makeText(getContext(), "Vocab Fragment Opened!", Toast.LENGTH_SHORT).show();

        // Use Activity content view as stable root for keyboard detection
        if (getActivity() != null) {
            keyboardRootView = requireActivity().findViewById(android.R.id.content);
        } else {
            keyboardRootView = root; // fallback
        }

        // Keyboard visibility listener: hide bottom menu and FAB when keyboard is shown
        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean lastStateVisible = false;

            @Override
            public void onGlobalLayout() {
                if (keyboardRootView == null || getActivity() == null) return;

                Rect r = new Rect();
                // safe: keyboardRootView is activity content view
                keyboardRootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = keyboardRootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;

                if (isKeyboardVisible == lastStateVisible) return; // no change
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

        // RecyclerView setup: use TopicCardAdapter
        java.util.List<TopicCard> allTopics = new java.util.ArrayList<>();
        allTopics.add(new TopicCard("Fruits", "Easy", 10, R.drawable.fruits));
        allTopics.add(new TopicCard("Information & Technology", "Medium", 12, R.drawable.it));
        allTopics.add(new TopicCard("Careers", "Hard", 8, R.drawable.careers));
        allTopics.add(new TopicCard("Apperances", "Easy", 9, R.drawable.apperances));
        allTopics.add(new TopicCard("Personalities", "Medium", 11, R.drawable.personalities));
        allTopics.add(new TopicCard("Travel", "Easy", 15, R.drawable.travel));

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        binding.recyclerTopics.setLayoutManager(lm);
        TopicCardAdapter topicAdapter = new TopicCardAdapter(allTopics, (item, pos) -> onCardClicked(pos+1));
        binding.recyclerTopics.setAdapter(topicAdapter);

        // Update: make search logic consistent with VocabFragment2
        binding.searchTopic.setOnClickListener(v -> {
            String query = binding.searchEditText.getText().toString().trim().toLowerCase();

            // If empty, behave like VocabFragment2: show all (there's no list adapter here) and inform the user
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "Showing all topics", Toast.LENGTH_SHORT).show();
                topicAdapter.updateList(new java.util.ArrayList<>(allTopics));
                return;
            }

            java.util.List<TopicCard> filtered = new java.util.ArrayList<>();
            for (TopicCard t : allTopics) {
                if (t.title != null && t.title.toLowerCase().contains(query)) {
                    filtered.add(t);
                }
            }

            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "No topic found for: " + query, Toast.LENGTH_SHORT).show();
            }
            topicAdapter.updateList(filtered);
        });


        binding.searchEditText.setOnClickListener(v -> {
            // Use KeyboardUtils to focus and show keyboard
            KeyboardUtils.showKeyboard(requireActivity(), binding.searchEditText);
        });

        binding.filter.setOnClickListener(v ->
        {
            Toast.makeText(requireContext(), "Filter clicked", Toast.LENGTH_SHORT).show();
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(),
                    v,
                    keyboardRootView,
                    keyboardListener
            );
        });

        // Standardize behavior: hide keyboard first, then pop backstack or navigate home as a fallback
        View.OnClickListener preClick = v -> {
            if (!isAdded()) return;
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(),
                    v,
                    keyboardRootView,
                    keyboardListener
            );
        };

        // Make sure to return to home fragment properly
        binding.btnReturn.setOnClickListener(v -> {
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(), v, keyboardRootView, keyboardListener);

            // prefer using MainActivity helper to keep BottomNavigationView state in sync
            if (requireActivity() instanceof com.example.nt118_englishvocabapp.MainActivity) {
                ((com.example.nt118_englishvocabapp.MainActivity) requireActivity()).navigateToHome();
                return;
            }

            // fallback (should rarely run)
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                AppCompatActivity activity = (AppCompatActivity) requireActivity();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new com.example.nt118_englishvocabapp.ui.home.HomeFragment())
                        .commitAllowingStateLoss();
            }
        });

        // individual card clicks handled by adapter callback

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener from the same view it was added to
        if (keyboardRootView != null && keyboardListener != null) {
            keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
        binding = null;
        keyboardRootView = null;
        keyboardListener = null;
    }

    private void onCardClicked(int index) {
        try {
            // Use KeyboardUtils to hide keyboard, remove listener and restore bottom UI
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(),
                    requireActivity().getWindow().getDecorView(),
                    keyboardRootView,
                    keyboardListener
            );

            // Navigate to detail fragment
            VocabFragment2 frag = new VocabFragment2();
            Bundle args = new Bundle();
            args.putInt("topic_index", index);
            frag.setArguments(args);

            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            String backStackName = "VocabFragment2_BackStack"; // make a named back stack entry
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, frag)
                    // Instead of addToBackStack(null), using a named back stack entry
                    .addToBackStack(backStackName)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error while opening topic " + index, e);
            Toast.makeText(requireContext(), "Navigation error: " + e.getClass().getSimpleName() + " - " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
