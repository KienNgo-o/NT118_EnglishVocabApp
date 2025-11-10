// ui/vocab/VocabFragment.java
package com.example.nt118_englishvocabapp.ui.vocab;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.adapters.VocabTopicAdapter;
import com.example.nt118_englishvocabapp.databinding.FragmentVocabBinding;
import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.ui.vocab2.VocabFragment2;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;

import java.util.ArrayList;
import java.util.List;

public class VocabFragment extends Fragment implements VocabTopicAdapter.OnTopicClickListener {

    private static final String TAG = "VocabFragment";
    private FragmentVocabBinding binding;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private int previousSoftInputMode = -1;

    private List<Topic> allTopics = new ArrayList<>();
    private VocabTopicAdapter topicAdapter;
    private VocabViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVocabBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Lưu và set chế độ bàn phím chỉ cho fragment này
        if (getActivity() != null && getActivity().getWindow() != null) {
            previousSoftInputMode = getActivity().getWindow().getAttributes().softInputMode;
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        // Gốc để phát hiện bàn phím
        keyboardRootView = getActivity() != null
                ? requireActivity().findViewById(android.R.id.content)
                : root;

        setupKeyboardListener();
        setupRecyclerView();

        viewModel = new ViewModelProvider(requireActivity()).get(VocabViewModel.class);
        observeViewModel();
        viewModel.fetchTopics();

        setupClickListeners();

        return root;
    }

    private void setupKeyboardListener() {
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
    }

    private void setupRecyclerView() {
        topicAdapter = new VocabTopicAdapter(this, requireContext());
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        binding.recyclerTopics.setLayoutManager(lm);
        binding.recyclerTopics.setAdapter(topicAdapter);
        if (binding.recyclerTopics.getItemAnimator() instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) binding.recyclerTopics.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        binding.recyclerTopics.setHasFixedSize(true);
    }

    private void observeViewModel() {
        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> {
            if (topics != null && !topics.isEmpty()) {
                allTopics.clear();
                allTopics.addAll(topics);
                topicAdapter.submitList(new ArrayList<>(allTopics));
            } else {
                allTopics.clear();
                topicAdapter.submitList(new ArrayList<>());
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Vocab load error: " + err);
            }
        });
    }

    private void setupClickListeners() {
        binding.searchTopic.setOnClickListener(v -> {
            String query = binding.searchEditText.getText().toString().trim().toLowerCase();
            filterTopics(query);
        });

        binding.searchEditText.setOnClickListener(v ->
                KeyboardUtils.showKeyboard(requireActivity(), binding.searchEditText)
        );

        binding.filter.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Filter clicked", Toast.LENGTH_SHORT).show();
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(), v, keyboardRootView, keyboardListener);
            try {
                FilterDialog filterSheet = new FilterDialog();
                if (getActivity() != null) {
                    filterSheet.show(getActivity().getSupportFragmentManager(), "VocabFilterSheet");
                } else {
                    filterSheet.show(getParentFragmentManager(), "VocabFilterSheet");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error showing filter sheet", e);
            }
        });

        binding.btnReturn.setOnClickListener(v -> {
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(), v, keyboardRootView, keyboardListener);
            if (requireActivity() instanceof com.example.nt118_englishvocabapp.MainActivity) {
                ((com.example.nt118_englishvocabapp.MainActivity) requireActivity()).navigateToHome();
            } else if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Lắng nghe bộ lọc từ FilterDialog
        getParentFragmentManager().setFragmentResultListener("vocabFilter", this, (key, bundle) -> {
            if (bundle == null) return;
            boolean savedOnly = bundle.getBoolean("savedOnly", false);
            String difficulty = bundle.getString("difficulty", null);
            applyFilters(savedOnly, difficulty);
        });
    }

    private void filterTopics(String query) {
        List<Topic> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            filtered.addAll(allTopics);
            Toast.makeText(requireContext(), "Showing all topics", Toast.LENGTH_SHORT).show();
        } else {
            for (Topic t : allTopics) {
                if (t.getTopicName() != null && t.getTopicName().toLowerCase().contains(query)) {
                    filtered.add(t);
                }
            }
            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "No topic found for: " + query, Toast.LENGTH_SHORT).show();
            }
        }
        topicAdapter.submitList(filtered);
    }

    private void applyFilters(boolean savedOnly, String difficulty) {
        List<Topic> filtered = new ArrayList<>();
        for (Topic t : allTopics) {
            boolean matchesSaved = !savedOnly && t.isSaved();
            boolean matchesDiff = (difficulty == null || difficulty.isEmpty()
                    || (t.getDifficulty() != null && t.getDifficulty().equalsIgnoreCase(difficulty)));
            if (matchesSaved && matchesDiff) filtered.add(t);
        }
        topicAdapter.submitList(filtered);
        Toast.makeText(requireContext(), "Filters applied", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTopicClick(Topic topic) {
        if ("locked".equalsIgnoreCase(topic.getStatus())) {
            Toast.makeText(getContext(), topic.getTopicName() + " is locked!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(),
                    requireActivity().getWindow().getDecorView(),
                    keyboardRootView,
                    keyboardListener
            );

            VocabFragment2 frag = new VocabFragment2();
            Bundle args = new Bundle();
            args.putInt("topic_index", topic.getTopicId());
            frag.setArguments(args);

            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, frag)
                    .addToBackStack("VocabFragment2_BackStack")
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Navigation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onTopicSaveClick(Topic topic, boolean isSaved) {
        Toast.makeText(getContext(),
                (isSaved ? "Saved " : "Unsaved ") + topic.getTopicName(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() != null && previousSoftInputMode != -1) {
            getActivity().getWindow().setSoftInputMode(previousSoftInputMode);
        }
        if (keyboardRootView != null && keyboardListener != null) {
            keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
        binding = null;
        keyboardRootView = null;
        keyboardListener = null;
    }
}
