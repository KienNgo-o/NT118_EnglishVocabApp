package com.example.nt118_englishvocabapp.ui.vocab3;

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
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.ui.vocab2.VocabFragment2;
import com.example.nt118_englishvocabapp.ui.vocab4.VocabFragment4;
import com.example.nt118_englishvocabapp.ui.vocab5.VocabFragment5;

/**
 * VocabFragment3 - Definition tab with navigation to Forms (VocabFragment4) and Synonyms (VocabFragment5).
 * Uses a shared VocabWordViewModel so all three detail fragments can observe the same word details.
 */
public class VocabFragment3 extends Fragment {

    public static final String ARG_WORD = "arg_word";
    public static final String ARG_WORD_TYPE = "arg_word_type";
    public static final String ARG_DEFINITION = "arg_definition";

    public VocabFragment3() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vocab3, container, false);

        // Tab controls
        TextView tabDefinition = root.findViewById(R.id.tab_definition);
        TextView tabForms = root.findViewById(R.id.tab_forms);
        TextView tabSynonyms = root.findViewById(R.id.tab_synonyms);

        // Content containers
        View contentDefinition = root.findViewById(R.id.content_definition);
        View contentForms = root.findViewById(R.id.content_forms);
        View contentSynonyms = root.findViewById(R.id.content_synonyms);

        // Lower content views
        TextView wordText = root.findViewById(R.id.wordText);
        TextView wordType = root.findViewById(R.id.wordType);
        TextView definitionEn = root.findViewById(R.id.definitionEn);

        // Read args safely
        Bundle args = getArguments();
        String word = args != null ? args.getString(ARG_WORD) : null;
        String type = args != null ? args.getString(ARG_WORD_TYPE) : null;
        String def = args != null ? args.getString(ARG_DEFINITION) : null;

        if (word != null) wordText.setText(word);
        if (type != null) wordType.setText(type);
        if (def != null) definitionEn.setText(def);

        // Shared ViewModel (scoped to the activity) so other detail fragments can observe updates
        com.example.nt118_englishvocabapp.ui.vocab3.VocabWordViewModel vm = new ViewModelProvider(requireActivity()).get(com.example.nt118_englishvocabapp.ui.vocab3.VocabWordViewModel.class);
        vm.setWordDetails(word, type, def);
        vm.getWord().observe(getViewLifecycleOwner(), w -> { if (w != null && !w.isEmpty()) wordText.setText(w); });
        vm.getWordType().observe(getViewLifecycleOwner(), t -> { if (t != null && !t.isEmpty()) wordType.setText(t); });
        vm.getDefinition().observe(getViewLifecycleOwner(), d -> { if (d != null && !d.isEmpty()) definitionEn.setText(d); });

        // Host container id where fragments are replaced
        final int hostId = (container != null) ? container.getId() : android.R.id.content;

        // Tab click listener: definition stays in this fragment; other tabs navigate
        View.OnClickListener tabClick = v -> {
            if (v == tabDefinition) {
                setSelectedTab(v, tabDefinition, tabForms, tabSynonyms,
                        contentDefinition, contentForms, contentSynonyms);
            } else if (v == tabForms) {
                VocabFragment4 f = new VocabFragment4();
                Bundle b = new Bundle();
                b.putString(ARG_WORD, word);
                b.putString(ARG_WORD_TYPE, type);
                b.putString(ARG_DEFINITION, def);
                f.setArguments(b);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(hostId, f)
                        .addToBackStack(null)
                        .commit();
            } else if (v == tabSynonyms) {
                VocabFragment5 f = new VocabFragment5();
                Bundle b = new Bundle();
                b.putString(ARG_WORD, word);
                b.putString(ARG_WORD_TYPE, type);
                b.putString(ARG_DEFINITION, def);
                f.setArguments(b);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(hostId, f)
                        .addToBackStack(null)
                        .commit();
            }
        };

        tabDefinition.setOnClickListener(tabClick);
        tabForms.setOnClickListener(tabClick);
        tabSynonyms.setOnClickListener(tabClick);

        // Initialize selection
        setSelectedTab(tabDefinition, tabDefinition, tabForms, tabSynonyms,
                contentDefinition, contentForms, contentSynonyms);

        // Return button: implement deterministic navigation directly so it ALWAYS goes back to VocabFragment2
        ImageButton btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) {
            btnReturn.setOnClickListener(v -> {
                if (getActivity() == null) return;
                // guard against double clicks/races
                v.setEnabled(false);
                // Immediately navigate to VocabFragment2, ignoring any back stack
                getActivity().runOnUiThread(() -> {
                    androidx.fragment.app.FragmentManager fm = requireActivity().getSupportFragmentManager();
                    try {
                        // If state already saved, delay the replace slightly to avoid IllegalStateException
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

                        // Not saved: synchronously clear back stack and execute pending
                        try { fm.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE); } catch (Exception ignored) {}
                        try { fm.executePendingTransactions(); } catch (Exception ignored) {}

                        // Perform a safe replace with reordering allowed
                        fm.beginTransaction().setReorderingAllowed(true).replace(hostId, new VocabFragment2()).commitAllowingStateLoss();
                    } catch (Exception ignored) {
                        if (getActivity() != null) requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                });
            });
        }

        Toast.makeText(getContext(), "Vocab Fragment 3 Opened!", Toast.LENGTH_SHORT).show();
        return root;
    }

    // Toggle tab UI + content visibility
    private void setSelectedTab(View selected, TextView def, TextView forms, TextView syn,
                                View contentDef, View contentForms, View contentSyn) {
        def.setSelected(def == selected);
        forms.setSelected(forms == selected);
        syn.setSelected(syn == selected);

        def.setEnabled(true);
        forms.setEnabled(true);
        syn.setEnabled(true);

        contentDef.setVisibility(def == selected ? View.VISIBLE : View.GONE);
        contentForms.setVisibility(forms == selected ? View.VISIBLE : View.GONE);
        contentSyn.setVisibility(syn == selected ? View.VISIBLE : View.GONE);
    }
}
