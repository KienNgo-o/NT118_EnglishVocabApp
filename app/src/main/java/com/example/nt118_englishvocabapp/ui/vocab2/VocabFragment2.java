// ui/vocab2/VocabFragment2.java
package com.example.nt118_englishvocabapp.ui.vocab2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentVocab2Binding;
import com.example.nt118_englishvocabapp.models.VocabWord; // 👈 THÊM IMPORT NÀY
import com.example.nt118_englishvocabapp.ui.vocab3.VocabFragment3;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VocabFragment2 extends Fragment {
    private FragmentVocab2Binding binding;
    private VocabTopicAdapter adapter;

    // ❗️THAY ĐỔI: Dùng Model mới
    private List<Topic> fullTopics = new ArrayList<>(); // UI Model (Topic.java)
    private List<VocabWord> backendWords = new ArrayList<>(); // Data Model (VocabWord.java)

    private Vocab2ViewModel viewModel;
    private Integer topicIdArg = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVocab2Binding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Nếu có topic id được truyền vào
        if (getArguments() != null && getArguments().containsKey("topic_index")) {
            topicIdArg = getArguments().getInt("topic_index");
        }

        // RecyclerView setup
        binding.recyclerTopics.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VocabTopicAdapter(new ArrayList<>(fullTopics), (topic, position) -> {
            // Mở fragment_vocab3 khi click vào 1 từ
            VocabFragment3 fragment = new VocabFragment3();
            Bundle b = new Bundle();
            b.putString(VocabFragment3.ARG_WORD, topic.getWord());
            b.putString(VocabFragment3.ARG_WORD_TYPE, topic.getWordType());
            b.putString(VocabFragment3.ARG_DEFINITION, topic.getDefinition());
            fragment.setArguments(b);

            int hostId = (container != null) ? container.getId() : android.R.id.content;
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(hostId, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.recyclerTopics.setAdapter(adapter);

        // ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(Vocab2ViewModel.class);
        observeViewModel();

        // Nếu có topicIdArg thì fetch backend, ngược lại dùng sample
        if (topicIdArg != null && topicIdArg > 0) {
            viewModel.fetchWords(topicIdArg);
        } else {
            fullTopics = new ArrayList<>(Arrays.asList(
                    new Topic("cat", "(n.)", "A small domesticated carnivorous mammal."),
                    new Topic("run", "(v.)", "To move at a speed faster than a walk."),
                    new Topic("beautiful", "(adj.)", "Pleasing the senses or mind aesthetically."),
                    new Topic("happiness", "(n.)", "The state of being happy."),
                    new Topic("eat", "(v.)", "To consume food."),
                    new Topic("technology", "(n.)", "Application of scientific knowledge for practical purposes."),
                    new Topic("travel", "(v.)", "To make a journey, typically of some length."),
                    new Topic("school", "(n.)", "An institution for educating children or adults."),
                    new Topic("weather", "(n.)", "The state of the atmosphere at a place and time."),
                    new Topic("sport", "(n.)", "An activity involving physical exertion and skill.")
            ));
            adapter.updateList(new ArrayList<>(fullTopics));
        }

        // Search icon click
        binding.searchTopic.setOnClickListener(v -> {
            String query = binding.searchEditText.getText().toString().trim().toLowerCase();
            if (query.isEmpty()) {
                adapter.updateList(new ArrayList<>(fullTopics));
                Toast.makeText(requireContext(), "Showing all words", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Topic> filtered = new ArrayList<>();
            for (Topic t : fullTopics) {
                String w = t.getWord() != null ? t.getWord().toLowerCase() : "";
                String d = t.getDefinition() != null ? t.getDefinition().toLowerCase() : "";
                if (w.contains(query) || d.contains(query)) {
                    filtered.add(t);
                }
            }

            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "No word found for: " + query, Toast.LENGTH_SHORT).show();
            }
            adapter.updateList(filtered);
        });

        // Nhận kết quả filter
        getParentFragmentManager().setFragmentResultListener("vocabFilter", this, (requestKey, bundle) -> {
            if (bundle == null) return;
            boolean savedOnly = bundle.getBoolean("savedOnly", false);
            String difficulty = bundle.getString("difficulty", null);
            applyFilters(savedOnly, difficulty);
        });

        // Nút filter
        binding.filter.setOnClickListener(v -> {
            FilterDialog filterSheet = new FilterDialog();
            try {
                if (getActivity() != null) {
                    filterSheet.show(getActivity().getSupportFragmentManager(), "Vocab2FilterSheet");
                } else {
                    filterSheet.show(getParentFragmentManager(), "Vocab2FilterSheet");
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error opening filter: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
        });

        // Nút quay lại
        ReturnButtonHelper.bind(binding.getRoot(), this, null, () -> requireActivity().finish());
        View btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) {
            btnReturn.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        Toast.makeText(getContext(), "Vocab Fragment 2 Opened!", Toast.LENGTH_SHORT).show();
        return root;
    }

    // ❗️THAY ĐỔI: ViewModel getter mới
    private void observeViewModel() {
        viewModel.getWordList().observe(getViewLifecycleOwner(), words -> { // 👈 Sửa ở đây
            if (binding == null) return;

            if (words == null) {
                binding.progressLoading.setVisibility(View.VISIBLE);
                return;
            }

            binding.progressLoading.setVisibility(View.GONE);
            backendWords.clear();
            backendWords.addAll(words);

            // Map từ VocabWord -> Topic (UI Model)
            fullTopics.clear();
            for (VocabWord w : backendWords) {
                Topic uiTopic = new Topic(
                        w.getWordText(),
                        "(n.)", // 👈 API 2 chỉ trả về danh từ
                        w.getPrimaryDefinition()
                );
                fullTopics.add(uiTopic);
            }

            adapter.updateList(new ArrayList<>(fullTopics));

            if (fullTopics.isEmpty()) {
                Toast.makeText(requireContext(), "No words found for this topic", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                if (binding != null) binding.progressLoading.setVisibility(View.GONE);
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters(boolean savedOnly, String difficulty) {
        if (fullTopics == null) return;
        List<Topic> filtered = new ArrayList<>();

        String wantedForm = difficulty == null ? null : difficulty.trim().toLowerCase();

        for (Topic t : fullTopics) {
            boolean matchesSaved = !savedOnly || t.isFavorite();
            boolean matchesForm = true;
            if (wantedForm != null && !wantedForm.isEmpty()) {
                String normalized = normalizeWordForm(t.getWordType());
                matchesForm = normalized != null && normalized.equals(wantedForm.toLowerCase());
            }
            if (matchesSaved && matchesForm) filtered.add(t);
        }

        adapter.updateList(filtered);
        String label = "Filters applied";
        if (savedOnly) label += ": favorite";
        if (difficulty != null) label += ", form=" + difficulty;
        Toast.makeText(requireContext(), label, Toast.LENGTH_SHORT).show();
    }

    private String normalizeWordForm(String raw) {
        if (raw == null) return null;
        String s = raw.toLowerCase().replaceAll("[^a-z]", "");
        if (s.isEmpty()) return null;
        if (s.startsWith("n")) return "noun";
        if (s.startsWith("v")) return "verb";
        if (s.startsWith("adj")) return "adjective";
        if (s.startsWith("adv")) return "adverb";
        if (s.contains("noun")) return "noun";
        if (s.contains("verb")) return "verb";
        if (s.contains("adjective")) return "adjective";
        if (s.contains("adverb")) return "adverb";
        return s;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
        fullTopics = null;
    }
}
