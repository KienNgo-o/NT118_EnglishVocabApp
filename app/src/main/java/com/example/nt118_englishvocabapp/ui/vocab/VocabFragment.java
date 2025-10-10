package com.example.nt118_englishvocabapp.ui.vocab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.databinding.FragmentVocabBinding;

public class VocabFragment extends Fragment {

    private FragmentVocabBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        VocabViewModel vocabViewModel =
                new ViewModelProvider(this).get(VocabViewModel.class);

        binding = FragmentVocabBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textVocab;
        vocabViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}