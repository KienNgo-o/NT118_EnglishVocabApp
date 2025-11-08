// java
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
import com.example.nt118_englishvocabapp.ui.vocab3.VocabFragment3;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VocabFragment2 extends Fragment {
    private FragmentVocab2Binding binding;
    private VocabTopicAdapter adapter;
    private List<Topic> fullTopics = new ArrayList<>();

    // ViewModel for backend-backed word list
    private Vocab2ViewModel viewModel;
    private Integer topicIdArg = null; // optional topic id passed from VocabFragment

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVocab2Binding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // If a topic id was passed, record it; otherwise we will use local samples
        if (getArguments() != null && getArguments().containsKey("topic_index")) {
            topicIdArg = getArguments().getInt("topic_index");
        }

        // RecyclerView setup
        binding.recyclerTopics.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VocabTopicAdapter(new ArrayList<>(fullTopics), (topic, position) -> {
            // Open fragment_vocab3 when any card is clicked and pass the selected word data
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

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(Vocab2ViewModel.class);
        observeViewModel();

        // If topicIdArg is provided, fetch from backend; else populate with sample data
        if (topicIdArg != null && topicIdArg > 0) {
            viewModel.fetchWords(topicIdArg);
        } else {
            // Sample words - replace with real data source later
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
                    new Topic("sport", "(n.)", "An activity involving physical exertion and skill in which an individual or team competes.")
            ));

            adapter.updateList(new ArrayList<>(fullTopics));
        }

        // Search icon click - filter the list
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

        // Listen for filter results from the vocab2 filter dialog (same key as VocabFragment)
        getParentFragmentManager().setFragmentResultListener("vocabFilter", this, (requestKey, bundle) -> {
            if (bundle == null) return;
            boolean savedOnly = bundle.getBoolean("savedOnly", false);
            String difficulty = bundle.getString("difficulty", null);
            applyFilters(savedOnly, difficulty);
        });

        // Filter icon - show vocab2 filter bottom sheet
        binding.filter.setOnClickListener(v -> {
            // Show the vocab2 FilterDialog (bottom sheet)
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

        // Standardized return behavior: fall back to finishing the activity if no backstack
        ReturnButtonHelper.bind(binding.getRoot(), this, null, () -> requireActivity().finish());
        View btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) {
            btnReturn.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
        Toast.makeText(getContext(), "Vocab Fragment 2 Opened!", Toast.LENGTH_SHORT).show();
        return root;
    }

    private void observeViewModel() {
        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> {
            if (topics == null) {
                // loading state - show progress
                if (binding != null) binding.progressLoading.setVisibility(View.VISIBLE);
                return;
            }

            // hide progress
            if (binding != null) binding.progressLoading.setVisibility(View.GONE);

            if (!topics.isEmpty()) {
                fullTopics.clear();
                fullTopics.addAll(topics);
                adapter.updateList(new ArrayList<>(fullTopics));
            } else {
                // empty result from backend
                fullTopics.clear();
                adapter.updateList(new ArrayList<>());
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
            boolean matchesSaved = !savedOnly || t.isFavorite(); // treat favorite as "saved"

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

    // Normalize examples like "(n.)", "n.", "noun", "(adj.)" into: "noun","verb","adjective","adverb".
    private String normalizeWordForm(String raw) {
        if (raw == null) return null;
        String s = raw.toLowerCase().replaceAll("[^a-z]", ""); // remove non-letters
        if (s.isEmpty()) return null;
        if (s.startsWith("n")) return "noun"; // covers n, noun
        if (s.startsWith("v")) return "verb"; // covers v, verb
        if (s.startsWith("adj")) return "adjective"; // covers adj, adjective
        if (s.startsWith("adv")) return "adverb"; // covers adv, adverb
        // fallback: try full words
        if (s.contains("noun")) return "noun";
        if (s.contains("verb")) return "verb";
        if (s.contains("adjective")) return "adjective";
        if (s.contains("adverb")) return "adverb";
        return s; // return whatever remains
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
        fullTopics = null;
    }
}
