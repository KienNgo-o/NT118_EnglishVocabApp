// java
package com.example.nt118_englishvocabapp.ui.vocab;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentVocabBinding;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.ui.vocab2.VocabFragment2;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import android.util.Log;

public class VocabFragment extends Fragment {

    private static final String TAG = "VocabFragment";
    private FragmentVocabBinding binding;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        VocabViewModel vocabViewModel =
                new ViewModelProvider(this).get(VocabViewModel.class);

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

        binding.searchTopic.setOnClickListener(v -> {
            String query = binding.searchEditText.getText().toString().trim().toLowerCase();

            if (query.isEmpty()) {
                // focus and open keyboard using KeyboardUtils
                KeyboardUtils.showKeyboard(requireActivity(), binding.searchEditText);
                return;
            }

            // Arrays of cards and their label TextViews (view binding generated names)
            View[] cards = new View[] {
                    binding.cardTopic1,
                    binding.cardTopic2,
                    binding.cardTopic3,
                    binding.cardTopic4,
                    binding.cardTopic5,
                    binding.cardTopic6
            };

            TextView[] labels = new TextView[] {
                    binding.txtTopic1,
                    binding.txtTopic2,
                    binding.txtTopic3,
                    binding.txtTopic4,
                    binding.txtTopic5,
                    binding.txtTopic6
            };

            int foundIndex = -1;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] != null) {
                    String labelText = labels[i].getText().toString().toLowerCase();
                    if (labelText.contains(query)) {
                        foundIndex = i;
                        break;
                    }
                }
            }

            if (foundIndex >= 0) {
                // Use KeyboardUtils to hide keyboard / remove listener / restore bottom UI
                keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                        requireActivity(),
                        v,
                        keyboardRootView,
                        keyboardListener
                );

                final View targetCard = cards[foundIndex];
                // scroll after layout pass to ensure coordinates are ready
                binding.scrollTopics.post(() -> {
                    int top = targetCard.getTop();
                    binding.scrollTopics.smoothScrollTo(0, top);
                    // optional: brief visual feedback (flash elevation) - keep minimal
                    targetCard.animate().alpha(0.9f).setDuration(120).withEndAction(() ->
                            targetCard.animate().alpha(1f).setDuration(120)
                    );
                });

            } else {
                Toast.makeText(requireContext(), "No topic found for: " + query, Toast.LENGTH_SHORT).show();
            }
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

        binding.btnReturn.setOnClickListener(v -> {
            if (!isAdded()) return;

            // Use KeyboardUtils to hide keyboard, remove listener and restore bottom UI
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(),
                    v,
                    keyboardRootView,
                    keyboardListener
            );

            // 4) Navigate back to HomeFragment
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                AppCompatActivity activity = (AppCompatActivity) requireActivity();
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new HomeFragment())
                        .commitAllowingStateLoss();
            }
        });

        binding.cardTopic1.setOnClickListener(v -> onCardClicked(1));
        binding.cardTopic2.setOnClickListener(v -> onCardClicked(2));
        binding.cardTopic3.setOnClickListener(v -> onCardClicked(3));
        binding.cardTopic4.setOnClickListener(v -> onCardClicked(4));
        binding.cardTopic5.setOnClickListener(v -> onCardClicked(5));
        binding.cardTopic6.setOnClickListener(v -> onCardClicked(6));

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
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, frag)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();

        } catch (Exception e) {
            Log.e(TAG, "Navigation error while opening topic " + index, e);
            Toast.makeText(requireContext(), "Navigation error: " + e.getClass().getSimpleName() + " - " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
