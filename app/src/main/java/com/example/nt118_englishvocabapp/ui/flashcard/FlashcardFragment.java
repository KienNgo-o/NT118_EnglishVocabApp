package com.example.nt118_englishvocabapp.ui.flashcard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.databinding.FragmentFlashcardBinding;

public class FlashcardFragment extends Fragment {

    private FragmentFlashcardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        FlashcardViewModel flashcardViewModel =
                new ViewModelProvider(this).get(FlashcardViewModel.class);

        binding = FragmentFlashcardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textFlashcard;
        flashcardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}