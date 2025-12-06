package com.example.nt118_englishvocabapp.ui.pronounce;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.PronounceWord;
import com.example.nt118_englishvocabapp.ui.flashcard.FlashcardViewModel;

import java.util.List;

public class PronounceWordlistFragment extends Fragment {

    private FlashcardViewModel viewModel;
    private PronounceWordAdapter adapter;
    private ProgressBar progress;

    public PronounceWordlistFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pronounce_wordlist, container, false);

        ImageButton btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) btnReturn.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        RecyclerView rv = root.findViewById(R.id.recycler_pronounce_topics);
        progress = root.findViewById(R.id.progress_pronounce_loading);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new PronounceWordAdapter(wordId -> {
                // Navigate to PronounceFragment for this word
                PronounceFragment dest = new PronounceFragment();
                Bundle b = new Bundle();
                b.putInt("word_id", wordId);
                dest.setArguments(b);
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.frame_layout, dest)
                            .addToBackStack(null)
                            .commit();
                }
            });
            rv.setAdapter(adapter);
        }

        // ViewModel: reuse FlashcardViewModel used in PronounceTopicFragment
        viewModel = new ViewModelProvider(requireActivity()).get(FlashcardViewModel.class);

        // Read arguments
        int topicId = -1;
        String topicName = null;
        Bundle args = getArguments();
        if (args != null) {
            topicId = args.getInt("topic_id", -1);
            topicName = args.getString("topic_name");
        }

        // Set header title to "Pronounce {topicName}" when provided
        android.widget.TextView tv = root.findViewById(R.id.txt_title);
        if (tv != null) {
            if (topicName != null && !topicName.isEmpty()) tv.setText("Pronounce " + topicName);
            else tv.setText(R.string.title_pronounce);
        }

        // Observe pronounce-ready words list
        viewModel.getPronounceWords().observe(getViewLifecycleOwner(), (List<PronounceWord> list) -> {
            if (progress != null) progress.setVisibility(list == null ? View.VISIBLE : View.GONE);
            if (adapter != null) adapter.submitList(list);
        });

        // Trigger fetch for this topic
        if (topicId >= 0) {
            viewModel.fetchWordsForPronounce(topicId);
        } else {
            // invalid topic id: post empty list to hide progress
            if (progress != null) progress.setVisibility(View.GONE);
            if (adapter != null) adapter.submitList(null);
        }

        return root;
    }
}
