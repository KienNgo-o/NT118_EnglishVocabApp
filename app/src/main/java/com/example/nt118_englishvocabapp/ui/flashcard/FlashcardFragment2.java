package com.example.nt118_englishvocabapp.ui.flashcard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.databinding.FragmentFlashcard2Binding;

public class FlashcardFragment2 extends Fragment {
    private FragmentFlashcard2Binding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFlashcard2Binding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Toast.makeText(getContext(), "Flashcard detail opened", Toast.LENGTH_SHORT).show();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
