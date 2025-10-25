// Java
package com.example.nt118_englishvocabapp.ui.vocab2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.databinding.FragmentVocab2Binding;

public class VocabFragment2 extends Fragment {
    private FragmentVocab2Binding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVocab2Binding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Toast.makeText(getContext(), "Vocab Fragment 2 Opened!", Toast.LENGTH_SHORT).show();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
