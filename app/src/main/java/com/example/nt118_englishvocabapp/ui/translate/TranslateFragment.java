package com.example.nt118_englishvocabapp.ui.translate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.databinding.FragmentTranslateBinding;

public class TranslateFragment extends Fragment {

    private FragmentTranslateBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TranslateViewModel translateViewModel =
                new ViewModelProvider(this).get(TranslateViewModel.class);

        binding = FragmentTranslateBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textTranslate;
        translateViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}