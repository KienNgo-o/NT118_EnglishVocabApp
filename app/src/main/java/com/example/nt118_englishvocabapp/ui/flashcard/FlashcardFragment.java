package com.example.nt118_englishvocabapp.ui.flashcard;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentFlashcardBinding;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.adapters.TopicAdapter;
public class FlashcardFragment extends Fragment {

    private FragmentFlashcardBinding binding;
    private FlashcardViewModel viewModel;
    private TopicAdapter topicAdapter;
    //private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFlashcardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        viewModel = new ViewModelProvider(this).get(FlashcardViewModel.class);
        Toast.makeText(getContext(), "Flashcard Fragment Opened!", Toast.LENGTH_SHORT).show();
        // stable root for keyboard detection
        if (getActivity() != null) {
            keyboardRootView = requireActivity().findViewById(android.R.id.content);
        } else {
            keyboardRootView = root;
        }

        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean lastVisible = false;
            @Override
            public void onGlobalLayout() {
                if (keyboardRootView == null || getActivity() == null) return;
                Rect r = new Rect();
                keyboardRootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = keyboardRootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                boolean isVisible = keypadHeight > screenHeight * 0.15;
                if (isVisible == lastVisible) return;
                lastVisible = isVisible;
                View bottomAppBar = requireActivity().findViewById(R.id.bottomAppBar);
                View fab = requireActivity().findViewById(R.id.fab);
                if (bottomAppBar != null) bottomAppBar.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                if (fab != null) fab.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            }
        };

        if (keyboardRootView != null) {
            keyboardRootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        }

        // prepare arrays of cards and labels
        View[] cards = new View[] {
                binding.cardFlash1, binding.cardFlash2, binding.cardFlash3,
                binding.cardFlash4, binding.cardFlash5, binding.cardFlash6
        };

        TextView[] labels = new TextView[] {
                binding.txtFlash1, binding.txtFlash2, binding.txtFlash3,
                binding.txtFlash4, binding.txtFlash5, binding.txtFlash6
        };

        // perform search logic now extracted to method-like lambda
        Runnable[] noop = new Runnable[1]; // holder for closure use if needed (not used)
        java.util.function.BiConsumer<String, View> performSearch = (queryRaw, triggerView) -> {
            String q = queryRaw == null ? "" : queryRaw.trim().toLowerCase();
            if (q.isEmpty()) {
                // show all cards and show keyboard for further input
                for (View c : cards) if (c != null) c.setVisibility(View.VISIBLE);
                KeyboardUtils.showKeyboard(requireActivity(), binding.searchEditText);
                return;
            }

            int matchCount = 0;
            int firstMatch = -1;
            for (int i = 0; i < labels.length; i++) {
                TextView lbl = labels[i];
                View card = cards[i];
                if (lbl != null && card != null) {
                    String title = lbl.getText().toString().toLowerCase();
                    if (title.contains(q)) {
                        card.setVisibility(View.VISIBLE);
                        if (firstMatch == -1) firstMatch = i;
                        matchCount++;
                    } else {
                        card.setVisibility(View.GONE);
                    }
                }
            }

            if (matchCount == 0) {
                // hide keyboard and restore UI then show toast
                keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                        requireActivity(), triggerView, keyboardRootView, keyboardListener);
                Toast.makeText(requireContext(), "No topic found: " + q, Toast.LENGTH_SHORT).show();
                return;
            }

            // if we have matches, hide keyboard and scroll to first match with animation
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(), triggerView, keyboardRootView, keyboardListener);
            final View target = cards[firstMatch];
            binding.scrollTopics.post(() -> {
                int top = target.getTop();
                binding.scrollTopics.smoothScrollTo(0, top);
                target.animate().alpha(0.9f).setDuration(120).withEndAction(() ->
                        target.animate().alpha(1f).setDuration(120));
            });
        };

        // click on search icon
        binding.searchFlashcard.setOnClickListener(v -> {
            String q = binding.searchEditText.getText().toString();
            performSearch.accept(q, v);
        });

        // IME search action
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = binding.searchEditText.getText().toString();
                performSearch.accept(q, binding.searchFlashcard);
                return true;
            }
            return false;
        });

        binding.searchEditText.setOnClickListener(v -> KeyboardUtils.showKeyboard(requireActivity(), binding.searchEditText));
        binding.filter.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Filter clicked", Toast.LENGTH_SHORT).show();
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(), v, keyboardRootView, keyboardListener);
        });

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

        // Card clicks navigate to blank FlashcardFragment2
        binding.cardFlash1.setOnClickListener(v -> openDetail(1));
        binding.cardFlash2.setOnClickListener(v -> openDetail(2));
        binding.cardFlash3.setOnClickListener(v -> openDetail(3));
        binding.cardFlash4.setOnClickListener(v -> openDetail(4));
        binding.cardFlash5.setOnClickListener(v -> openDetail(5));
        binding.cardFlash6.setOnClickListener(v -> openDetail(6));

        return root;
    }

    private void openDetail(int idx) {
        keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                requireActivity(), requireActivity().getWindow().getDecorView(), keyboardRootView, keyboardListener);
        com.example.nt118_englishvocabapp.ui.flashcard.FlashcardFragment2 frag = new com.example.nt118_englishvocabapp.ui.flashcard.FlashcardFragment2();
        Bundle b = new Bundle();
        b.putInt("topic_index", idx);
        frag.setArguments(b);
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, frag)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (keyboardRootView != null && keyboardListener != null) {
            keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
        binding = null;
        keyboardRootView = null;
        keyboardListener = null;
    }
}
