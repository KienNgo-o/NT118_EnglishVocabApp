package com.example.nt118_englishvocabapp.ui.vocab4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.ui.vocab3.VocabFragment3;
import com.example.nt118_englishvocabapp.ui.vocab3.VocabWordViewModel;
import com.example.nt118_englishvocabapp.ui.vocab5.VocabFragment5;
import com.example.nt118_englishvocabapp.ui.vocab2.VocabFragment2;

/**
 * VocabFragment4 (Forms) - currently shows forms content and allows navigation
 * between definition/forms/synonyms.
 */
public class VocabFragment4 extends Fragment {

    public VocabFragment4() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vocab4, container, false);

        TextView tabDefinition = root.findViewById(R.id.tab_definition);
        TextView tabForms = root.findViewById(R.id.tab_forms);
        TextView tabSynonyms = root.findViewById(R.id.tab_synonyms);

        // Mark Forms as active
        tabForms.setSelected(true);
        tabDefinition.setSelected(false);
        tabSynonyms.setSelected(false);
        // Keep all tabs enabled so they remain clickable
        tabForms.setEnabled(true);
        tabDefinition.setEnabled(true);
        tabSynonyms.setEnabled(true);

        // Populate basic word info from args
        TextView wordText = root.findViewById(R.id.wordText);
        TextView wordType = root.findViewById(R.id.wordType);
        Bundle args = getArguments();
        if (args != null) {
            String word = args.getString(VocabFragment3.ARG_WORD);
            String type = args.getString(VocabFragment3.ARG_WORD_TYPE);
            if (word != null) wordText.setText(word);
            if (type != null) wordType.setText(type);

            // Seed shared ViewModel so other detail fragments can observe/update
            VocabWordViewModel vm = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(VocabWordViewModel.class);
            vm.setWordDetails(word, type, null);
            // Observe VM for future backend updates
            vm.getWord().observe(getViewLifecycleOwner(), w -> { if (w != null && !w.isEmpty()) wordText.setText(w); });
            vm.getWordType().observe(getViewLifecycleOwner(), t -> { if (t != null && !t.isEmpty()) wordType.setText(t); });
        }

        int hostId = (container != null) ? container.getId() : android.R.id.content;

        tabDefinition.setOnClickListener(v -> {
            VocabFragment3 f = new VocabFragment3();
            // pass args through
            f.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(hostId, f)
                    .addToBackStack(null)
                    .commit();
        });

        tabForms.setOnClickListener(v -> {
            // already on Forms (this fragment) - no-op or provide subtle feedback
        });

        tabSynonyms.setOnClickListener(v -> {
            VocabFragment5 f = new VocabFragment5();
            f.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(hostId, f)
                    .addToBackStack(null)
                    .commit();
        });

        // Return button: always navigate immediately to VocabFragment2, ignoring back stack
        ImageButton btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) {
            btnReturn.setOnClickListener(v -> {
                if (getActivity() == null) return;
                v.setEnabled(false);
                getActivity().runOnUiThread(() -> {
                    androidx.fragment.app.FragmentManager fm = requireActivity().getSupportFragmentManager();
                    try {
                        if (fm.isStateSaved()) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    fm.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                    fm.executePendingTransactions();
                                    fm.beginTransaction().setReorderingAllowed(true).replace(hostId, new VocabFragment2()).commitAllowingStateLoss();
                                } catch (Exception ignored) {
                                    if (getActivity() != null) requireActivity().getOnBackPressedDispatcher().onBackPressed();
                                }
                            }, 120);
                            return;
                        }

                        try { fm.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE); } catch (Exception ignored) {}
                        try { fm.executePendingTransactions(); } catch (Exception ignored) {}
                        fm.beginTransaction().setReorderingAllowed(true).replace(hostId, new VocabFragment2()).commitAllowingStateLoss();
                    } catch (Exception ignored) {
                        if (getActivity() != null) requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                });
            });
        }

        Toast.makeText(getContext(), "Vocab Fragment 4 Opened!", Toast.LENGTH_SHORT).show();
        return root;
    }
}
