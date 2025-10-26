// app/src/main/java/com/example/nt118_englishvocabapp/ui/flashcard/FlashcardFragment.java
package com.example.nt118_englishvocabapp.ui.flashcard;

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

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentFlashcardBinding;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;

public class FlashcardFragment extends Fragment {

    private FragmentFlashcardBinding binding;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFlashcardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
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

        binding.searchFlashcard.setOnClickListener(v -> {
            String q = binding.searchEditText.getText().toString().trim().toLowerCase();
            if (q.isEmpty()) {
                KeyboardUtils.showKeyboard(requireActivity(), binding.searchEditText);
                return;
            }

            View[] cards = new View[] {
                    binding.cardFlash1, binding.cardFlash2, binding.cardFlash3,
                    binding.cardFlash4, binding.cardFlash5, binding.cardFlash6
            };

            TextView[] labels = new TextView[] {
                    binding.txtFlash1, binding.txtFlash2, binding.txtFlash3,
                    binding.txtFlash4, binding.txtFlash5, binding.txtFlash6
            };

            int found = -1;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] != null && labels[i].getText().toString().toLowerCase().contains(q)) {
                    found = i;
                    break;
                }
            }

            if (found >= 0) {
                keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                        requireActivity(), v, keyboardRootView, keyboardListener);
                View target = cards[found];
                binding.scrollTopics.post(() -> {
                    int top = target.getTop();
                    binding.scrollTopics.smoothScrollTo(0, top);
                    target.animate().alpha(0.9f).setDuration(120).withEndAction(() ->
                            target.animate().alpha(1f).setDuration(120));
                });
            } else {
                Toast.makeText(requireContext(), "No topic found: " + q, Toast.LENGTH_SHORT).show();
            }
        });

        binding.searchEditText.setOnClickListener(v -> KeyboardUtils.showKeyboard(requireActivity(), binding.searchEditText));
        binding.filter.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Filter clicked", Toast.LENGTH_SHORT).show();
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(), v, keyboardRootView, keyboardListener);
        });

        binding.btnReturn.setOnClickListener(v -> {
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(), v, keyboardRootView, keyboardListener);

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
