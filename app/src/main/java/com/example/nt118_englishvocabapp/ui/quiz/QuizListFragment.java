package com.example.nt118_englishvocabapp.ui.quiz;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.adapters.QuizListAdapter;
import com.example.nt118_englishvocabapp.databinding.FragmentQuizListBinding;
import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.ui.vocab.VocabViewModel;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;

import java.util.ArrayList;
import java.util.List;

public class QuizListFragment extends Fragment {

    private static final String TAG = "QuizListFragment";
    private FragmentQuizListBinding binding;
    private QuizListAdapter adapter;
    private VocabViewModel viewModel; // Tái sử dụng ViewModel của Vocab để lấy list Topic

    // Danh sách gốc để phục vụ việc tìm kiếm (filter)
    private List<Topic> fullTopicList = new ArrayList<>();

    public QuizListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 1. Setup ViewBinding
        binding = FragmentQuizListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 2. Setup ViewModel (Dùng chung với Activity để tận dụng dữ liệu đã tải nếu có)
        viewModel = new ViewModelProvider(requireActivity()).get(VocabViewModel.class);

        // 3. Setup RecyclerView & Adapter
        setupRecyclerView();

        // 4. Setup các sự kiện (Click, Search)
        setupListeners();

        // 5. Theo dõi dữ liệu
        observeViewModel();

        // Gọi API để tải danh sách mới nhất (để cập nhật trạng thái Locked/Unlocked)
        viewModel.fetchTopics();

        return root;
    }

    private void setupRecyclerView() {
        // Khởi tạo Adapter với Listener xử lý khi bấm nút "Start"
        adapter = new QuizListAdapter(topic -> {
            // Xử lý sự kiện khi bấm "Start" trên một Topic đã mở khóa
            navigateToQuizDetail(topic);
        });

        binding.recyclerQuizzes.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerQuizzes.setAdapter(adapter);
        binding.recyclerQuizzes.setHasFixedSize(true);
    }

    private void setupListeners() {
        // Nút Back
        binding.btnReturn.setOnClickListener(v -> {
            KeyboardUtils.hideKeyboard(requireActivity());
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                // Nếu không còn gì trong stack, có thể đóng Fragment hoặc về Home
                // Tùy vào logic điều hướng của bạn
                requireActivity().onBackPressed();
            }
        });

        // Logic Tìm kiếm (Search)
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        // Theo dõi danh sách Topic từ API
        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> {
            if (topics != null) {
                fullTopicList.clear();
                fullTopicList.addAll(topics);

                // Cập nhật lên giao diện
                adapter.submitList(new ArrayList<>(fullTopicList));

                if (topics.isEmpty()) {
                    Toast.makeText(getContext(), "No quizzes available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Theo dõi lỗi
        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading quizzes: " + errorMsg);
            }
        });
    }

    private void filterList(String query) {
        String text = query.toLowerCase().trim();
        List<Topic> filteredList = new ArrayList<>();

        if (text.isEmpty()) {
            filteredList.addAll(fullTopicList);
        } else {
            for (Topic item : fullTopicList) {
                if (item.getTopicName().toLowerCase().contains(text)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.submitList(filteredList);
    }

    /**
     * Hàm điều hướng sang màn hình làm bài thi
     */
    private void navigateToQuizDetail(Topic topic) {
        // TODO: CHÚNG TA SẼ THỰC HIỆN PHẦN NÀY Ở BƯỚC TIẾP THEO
        // Sẽ điều hướng đến QuizActivity hoặc QuizFragment
        // và truyền topic.getTopicId() sang.

        Toast.makeText(getContext(), "Starting Quiz: " + topic.getTopicName(), Toast.LENGTH_SHORT).show();


        QuizGameFragment quizFragment = new QuizGameFragment();
        Bundle args = new Bundle();
        args.putInt("topic_id", topic.getTopicId());
        quizFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, quizFragment)
                .addToBackStack(null)
                .commit();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Tránh memory leak
    }
}