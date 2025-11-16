package com.example.nt118_englishvocabapp.ui.flashcard;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.adapters.TopicAdapter;
import com.example.nt118_englishvocabapp.databinding.FragmentFlashcardBinding;
import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.ui.vocab.FilterDialog;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment hiển thị danh sách các chủ đề (Topics) học.
 * Dữ liệu được tải từ ViewModel và hiển thị bằng RecyclerView.
 * Implement TopicAdapter.OnTopicClickListener để xử lý sự kiện click.
 */
public class FlashcardFragment extends Fragment implements TopicAdapter.OnTopicClickListener {

    private FragmentFlashcardBinding binding;
    private FlashcardViewModel viewModel;
    private TopicAdapter topicAdapter;

    private final List<Topic> allTopics = new ArrayList<>(); // Danh sách đầy đủ để lọc
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private SharedPreferences prefs;
    private static final String TOPIC_SAVED_PREFS = "topic_saved_prefs";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFlashcardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 1. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(FlashcardViewModel.class);
        prefs = requireContext().getSharedPreferences(TOPIC_SAVED_PREFS, Context.MODE_PRIVATE);

        // 2. Cài đặt RecyclerView
        setupRecyclerView();

        // 3. Cài đặt các Listener (Keyboard, Search, Click...)
        setupKeyboardListener(root);
        setupClickListeners();

        // Listen for filter dialog results
        getParentFragmentManager().setFragmentResultListener("vocabFilter", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                boolean savedOnly = result.getBoolean("savedOnly", false);
                String difficulty = result.getString("difficulty", null);
                applyFilter(savedOnly, difficulty);
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 4. Bắt đầu theo dõi (Observe) ViewModel
        observeViewModel();

        // 5. Yêu cầu ViewModel tải dữ liệu
        // (AuthInterceptor sẽ tự động chèn token)
        viewModel.fetchTopics();
    }

    /**
     * Khởi tạo và cài đặt RecyclerView và Adapter
     */
    private void setupRecyclerView() {
        // 'this' (Fragment này) sẽ là trình xử lý click
        topicAdapter = new TopicAdapter(this);
        // Báo cho RecyclerView trong XML (phải đặt ID là topics_recycler_view)
        binding.topicsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.topicsRecyclerView.setAdapter(topicAdapter);
        // Reduce flicker by disabling change animations (updates handled with payloads)
        if (binding.topicsRecyclerView.getItemAnimator() instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) binding.topicsRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        binding.topicsRecyclerView.setHasFixedSize(true);
    }

    /**
     * Lắng nghe thay đổi dữ liệu từ ViewModel
     */
    private void observeViewModel() {
        // Theo dõi danh sách chủ đề
        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> {
            if (topics != null && !topics.isEmpty()) {
                Log.d("FlashcardFragment", "Topics đã tải: " + topics.size());
                // Debug: log every topic's id and wordCount for diagnosis
                try {
                    StringBuilder sb = new StringBuilder();
                    for (Topic t : topics) sb.append("[id=").append(t.getTopicId()).append(",cnt=").append(t.getWordCount()).append("],");
                    Log.d("FlashcardFragment", "Topics counts: " + sb.toString());
                } catch (Exception ignored) {}
                allTopics.clear();
                allTopics.addAll(topics); // Lưu danh sách đầy đủ để lọc
                // Submit with a commit callback so we can log when the adapter applies the diff.
                // As a safe fallback we call notifyDataSetChanged() to ensure word counts are visible
                topicAdapter.submitList(new ArrayList<>(topics), () -> {
                    try {
                        Log.d("FlashcardFragment", "Adapter commit completed. adapterSize=" + topicAdapter.getItemCount());
                        // Force full refresh once to guarantee UI shows updated word counts (harmless fallback)
                        topicAdapter.notifyDataSetChanged();
                    } catch (Exception ignored) {}
                }); // Gửi 1 bản copy cho adapter
            } else {
                Log.d("FlashcardFragment", "Không có topic nào hoặc list rỗng.");
                allTopics.clear();
                topicAdapter.submitList(new ArrayList<>());
            }
        });

        // Theo dõi thông báo lỗi
        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                Log.e("FlashcardFragment", "Lỗi: " + errorMsg);
            }
        });
    }

    /**
     * Cài đặt tất cả các trình xử lý click (trừ keyboard)
     */
    private void setupClickListeners() {
        // Nút Quay lại
        binding.btnReturn.setOnClickListener(v -> {
            KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);

            // Logic quay về Home (giữ nguyên của bạn)
            if (requireActivity() instanceof com.example.nt118_englishvocabapp.MainActivity) {
                ((com.example.nt118_englishvocabapp.MainActivity) requireActivity()).navigateToHome();
            } else {
                getParentFragmentManager().popBackStack();
            }
        });

        // Nút Lọc (Filter)
        binding.filter.setOnClickListener(v -> {
            // Open the existing Vocab FilterDialog (reuses fragment_vocab_filter layout & logic)
            FilterDialog dlg = new FilterDialog();
            dlg.show(getParentFragmentManager(), "vocab_filter_dialog");
            KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);
        });

        // Click vào thanh tìm kiếm
        binding.searchEditText.setOnClickListener(v ->
                KeyboardUtils.showKeyboard(requireActivity(), binding.searchEditText)
        );

        // Click vào icon tìm kiếm (kính lúp)
        View searchIcon = binding.getRoot().findViewById(R.id.search_flashcard);
        if (searchIcon != null) {
            searchIcon.setOnClickListener(v -> {
                String query = binding.searchEditText.getText().toString();
                filterTopics(query);
                KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);
            });
        }

        // Nhấn nút "Search" trên bàn phím
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.searchEditText.getText().toString();
                filterTopics(query);
                KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);
                return true;
            }
            return false;
        });
    }

    /**
     * Lọc danh sách chủ đề trên UI dựa trên query
     */
    private void filterTopics(String query) {
        String q = query.toLowerCase().trim();
        List<Topic> source = viewModel.getTopics().getValue();
        if (source == null) {
            // fallback to local cache
            source = new ArrayList<>(allTopics);
        }

        List<Topic> filteredList = new ArrayList<>();

        if (q.isEmpty()) {
            filteredList.addAll(source); // Hiển thị lại tất cả (use latest source)
        } else {
            for (Topic topic : source) {
                if (topic.getTopicName().toLowerCase().contains(q)) {
                    filteredList.add(topic);
                }
            }
        }

        topicAdapter.submitList(filteredList); // Cập nhật RecyclerView

        if (filteredList.isEmpty() && !q.isEmpty()) {
            Toast.makeText(requireContext(), "No topic found: " + query, Toast.LENGTH_SHORT).show();
        }
    }

    // Apply filter nhận từ FilterDialog
    private void applyFilter(boolean savedOnly, String difficulty) {
        List<Topic> source = viewModel.getTopics().getValue();
        if (source == null) source = new ArrayList<>(allTopics);

        List<Topic> filtered = new ArrayList<>();
        for (Topic t : source) {
            // check savedOnly
            if (savedOnly) {
                boolean saved = prefs.getBoolean("topic_saved_" + t.getTopicId(), false);
                if (!saved) continue; // skip non-saved
            }
            // check difficulty filter
            if (difficulty != null && !difficulty.isEmpty()) {
                String diff = t.getDifficulty() != null ? t.getDifficulty().trim() : "";
                if (diff.isEmpty() || !diff.equalsIgnoreCase(difficulty)) continue;
            }
            filtered.add(t);
        }

        topicAdapter.submitList(filtered);
    }

    /**
     * Đây là hàm được gọi khi một item trong RecyclerView được click
     */
    @Override
    public void onTopicClick(Topic topic) {
        // Kiểm tra logic "khóa"
        if ("locked".equals(topic.getStatus())) {
            Toast.makeText(getContext(), topic.getTopicName() + " is locked!", Toast.LENGTH_SHORT).show();
            return; // Không làm gì cả
        }

        // Nếu không khóa, mở màn hình chi tiết
        openDetail(topic);
    }

    /**
     * Mở màn hình FlashcardFragment2 với topicId được chọn
     */
    private void openDetail(Topic topic) {
        KeyboardUtils.hideKeyboardAndRestoreUI(
                requireActivity(), requireActivity().getWindow().getDecorView(), keyboardRootView, keyboardListener);

        FlashcardFragment2 frag = new FlashcardFragment2();
        Bundle b = new Bundle();
        b.putInt("topic_index", topic.getTopicId()); // Gửi topicId thật qua Bundle
        // Also pass the topic name so the detail fragment can set the title immediately
        if (topic.getTopicName() != null) b.putString("topic_name", topic.getTopicName());
        frag.setArguments(b);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, frag)
                .addToBackStack(null) // Cho phép nhấn Back để quay lại
                .commit();
    }

    // ===================================================================
    // == PHẦN XỬ LÝ BÀN PHÍM (GIỮ NGUYÊN) ==
    // ===================================================================

    /**
     * Cài đặt listener để theo dõi bàn phím
     */
    private void setupKeyboardListener(View root) {
        if (getActivity() != null) {
            this.keyboardRootView = requireActivity().findViewById(android.R.id.content);
        } else {
            this.keyboardRootView = root;
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

                // Ẩn/hiện BottomAppBar
                View bottomAppBar = requireActivity().findViewById(R.id.bottomAppBar);
                View fab = requireActivity().findViewById(R.id.fab);
                if (bottomAppBar != null) bottomAppBar.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                if (fab != null) fab.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            }
        };

        if (this.keyboardRootView != null) {
            this.keyboardRootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Gỡ bỏ listener để tránh rò rỉ bộ nhớ
        if (keyboardRootView != null && keyboardListener != null) {
            keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
        binding = null;
        keyboardRootView = null;
        keyboardListener = null;
    }
}