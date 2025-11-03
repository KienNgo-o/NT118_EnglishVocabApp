// java
package com.example.nt118_englishvocabapp.ui.vocab2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentVocab2Binding;
import com.example.nt118_englishvocabapp.ui.vocab3.VocabFragment3;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VocabFragment2 extends Fragment {
    private FragmentVocab2Binding binding;
    private VocabTopicAdapter adapter;
    private List<Topic> fullTopics = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVocab2Binding.inflate(inflater, container, false);
        View root = binding.getRoot();

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

        // Filter icon - simple feedback for now
        binding.filter.setOnClickListener(v -> Toast.makeText(requireContext(), "Filter clicked", Toast.LENGTH_SHORT).show());

        // Standardized return behavior: fall back to finishing the activity if no backstack
        ReturnButtonHelper.bind(binding.getRoot(), this, null, () -> requireActivity().finish());
        View btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) {
            btnReturn.setOnClickListener(v -> {
                getParentFragmentManager().popBackStack();
            });
        }
        Toast.makeText(getContext(), "Vocab Fragment 2 Opened!", Toast.LENGTH_SHORT).show();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
        fullTopics = null;
    }
}
