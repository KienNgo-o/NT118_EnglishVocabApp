package com.example.nt118_englishvocabapp.ui.pronounce;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.Topic;
import com.example.nt118_englishvocabapp.ui.flashcard.FlashcardViewModel;

public class PronounceTopicFragment extends Fragment implements TopicTimelineAdapter.OnTopicClickListener {

    private FlashcardViewModel viewModel;
    private TopicTimelineAdapter adapter;

    public PronounceTopicFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pronounce_topic, container, false);

        ImageButton btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) btnReturn.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        RecyclerView rv = root.findViewById(R.id.recycler_timeline);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new TopicTimelineAdapter(this);
            rv.setAdapter(adapter);
            int lineColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.darker_gray);
            rv.addItemDecoration(new TimelineDecoration(lineColor, 2f));
        }

        viewModel = new ViewModelProvider(requireActivity()).get(FlashcardViewModel.class);
        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> {
            if (adapter != null) adapter.submitList(topics);
        });
        viewModel.fetchTopics();

        return root;
    }

    @Override
    public void onTopicClick(Topic topic, int position) {
        // Navigate to the wordlist fragment for the clicked topic, pass topic id/name
        if (getActivity() != null && topic != null) {
            PronounceWordlistFragment dest = new PronounceWordlistFragment();
            android.os.Bundle args = new android.os.Bundle();
            args.putInt("topic_id", topic.getTopicId());
            args.putString("topic_name", topic.getTopicName());
            dest.setArguments(args);

            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, dest)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
