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
import com.example.nt118_englishvocabapp.models.VocabWord; // 👈 THÊM IMPORT
import com.example.nt118_englishvocabapp.ui.vocab3.VocabFragment3;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

import com.example.nt118_englishvocabapp.ui.flashcard.FlashcardViewModel;
import com.example.nt118_englishvocabapp.models.LearnableItem;
import com.example.nt118_englishvocabapp.models.FlashcardItem;
import com.example.nt118_englishvocabapp.models.Definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VocabFragment2 extends Fragment {

    private FragmentVocab2Binding binding;
    private VocabTopicAdapter adapter;
    private List<Topic> fullTopics = new ArrayList<>();

    private Vocab2ViewModel viewModel;
    private FlashcardViewModel flashcardViewModel;
    private Integer topicIdArg = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVocab2Binding.inflate(inflater, container, false);

        // Nhận argument (nếu có)
        if (getArguments() != null && getArguments().containsKey("topic_index")) {
            topicIdArg = getArguments().getInt("topic_index");
        }

        // RecyclerView setup
        binding.recyclerTopics.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Adapter setup
        adapter = new VocabTopicAdapter(new ArrayList<>(fullTopics), (topic, position) -> {
            // Khi click vào 1 từ vựng, mở màn hình 3
            VocabFragment3 fragment = new VocabFragment3();
            Bundle b = new Bundle();
            b.putInt(VocabFragment3.ARG_WORD_ID, topic.getWordId());
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
        flashcardViewModel = new ViewModelProvider(requireActivity()).get(FlashcardViewModel.class);
        observeViewModel();

        // If we have a real topicId, prefer flashcards endpoint which contains POS/definitions
        // and may include multiple entries for the same word with different POS.
        if (topicIdArg != null && topicIdArg > 0) {
            // Observe learnable items (each LearnableItem = FlashcardItem + one Definition)
            flashcardViewModel.getLearnableItems().observe(getViewLifecycleOwner(), items -> {
                if (binding == null) return;

                if (items == null) {
                    binding.progressLoading.setVisibility(View.VISIBLE);
                    return;
                }

                binding.progressLoading.setVisibility(View.GONE);
                fullTopics.clear();

                for (LearnableItem li : items) {
                    if (li == null) continue;
                    FlashcardItem fi = li.word;
                    Definition def = li.definition;

                    String wordText = fi != null ? fi.getWordText() : "";
                    String defText = def != null ? def.getDefinitionText() : "";
                    String posLabel = "";
                    if (def != null && def.getPos() != null && def.getPos().getPosName() != null) {
                        posLabel = "(" + def.getPos().getPosName() + ")";
                    }

                    Topic uiTopic = new Topic(
                            fi != null ? fi.getWordId() : -1,
                            wordText,
                            posLabel,
                            defText
                    );
                    fullTopics.add(uiTopic);
                }

                // Debug: log mapped items to confirm duplicates per POS exist
                try {
                    StringBuilder dbg = new StringBuilder();
                    for (int i = 0; i < fullTopics.size(); i++) {
                        Topic t = fullTopics.get(i);
                        dbg.append(i).append(':').append(t.getWord()).append(t.getWordType()).append(',');
                    }
                    android.util.Log.d("VocabFragment2", "Mapped fullTopics (size=" + fullTopics.size() + "): " + dbg.toString());
                } catch (Exception ignored) {}

                adapter.updateList(new ArrayList<>(fullTopics));
                android.util.Log.d("VocabFragment2", "Adapter updated, adapterSize=" + adapter.getItemCount());
                 if (fullTopics.isEmpty()) {
                     Toast.makeText(requireContext(), "No words found for this topic", Toast.LENGTH_SHORT).show();
                 }
            });

            // Trigger fetch from flashcard endpoint
            flashcardViewModel.fetchFlashcards(topicIdArg);

        } else {
            // If no topicId provided, fallback to Vocab2ViewModel (demo/sample data)
            if (topicIdArg == null || topicIdArg <= 0) {
                fullTopics = new ArrayList<>(Arrays.asList(
                        new Topic("cat", "(n.)", "A small domesticated carnivorous mammal."),
                        new Topic("run", "(v.)", "To move at a speed faster than a walk."),
                        new Topic("beautiful", "(adj.)", "Pleasing the senses or mind aesthetically.")
                ));
                adapter.updateList(new ArrayList<>(fullTopics));
            }
        }

        // Tìm kiếm
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
                if (w.contains(query) || d.contains(query)) filtered.add(t);
            }

            if (filtered.isEmpty()) {
                Toast.makeText(requireContext(), "No word found for: " + query, Toast.LENGTH_SHORT).show();
            }
            adapter.updateList(filtered);
        });

        // Nhận filter result
        getParentFragmentManager().setFragmentResultListener("vocabFilter", this, (requestKey, bundle) -> {
            if (bundle == null) return;
            boolean savedOnly = bundle.getBoolean("savedOnly", false);
            String difficulty = bundle.getString("difficulty", null);
            applyFilters(savedOnly, difficulty);
        });

        // Mở filter dialog
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
        if (binding.btnReturn != null) {
            binding.btnReturn.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        Toast.makeText(getContext(), "Vocab Fragment 2 Opened!", Toast.LENGTH_SHORT).show();
        return binding.getRoot();
    }

    // Quan sát dữ liệu từ ViewModel
    private void observeViewModel() {
        viewModel.getWordList().observe(getViewLifecycleOwner(), words -> {
            if (binding == null) return;

            if (words == null) {
                binding.progressLoading.setVisibility(View.VISIBLE);
                return;
            }

            binding.progressLoading.setVisibility(View.GONE);

            fullTopics.clear();
            for (VocabWord w : words) {
                Topic uiTopic = new Topic(
                        w.getWordId(),
                        w.getWordText(),
                        "(n.)", // giả định loại từ mặc định là danh từ
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

    // Bộ lọc
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
