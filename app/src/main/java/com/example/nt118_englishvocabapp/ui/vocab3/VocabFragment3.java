// java
package com.example.nt118_englishvocabapp.ui.vocab3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class VocabFragment3 extends Fragment {

    public VocabFragment3() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(com.example.nt118_englishvocabapp.R.layout.fragment_vocab3, container, false);

        // Find tab views
        TextView tabDefinition = root.findViewById(com.example.nt118_englishvocabapp.R.id.tab_definition);
        TextView tabForms = root.findViewById(com.example.nt118_englishvocabapp.R.id.tab_forms);
        TextView tabSynonyms = root.findViewById(com.example.nt118_englishvocabapp.R.id.tab_synonyms);

        // Content containers
        View contentDefinition = root.findViewById(com.example.nt118_englishvocabapp.R.id.content_definition);
        View contentForms = root.findViewById(com.example.nt118_englishvocabapp.R.id.content_forms);
        View contentSynonyms = root.findViewById(com.example.nt118_englishvocabapp.R.id.content_synonyms);

        // Helper listener to set selection
        View.OnClickListener tabClick = v -> {
            setSelectedTab(v, tabDefinition, tabForms, tabSynonyms, contentDefinition, contentForms, contentSynonyms);
        };

        tabDefinition.setOnClickListener(tabClick);
        tabForms.setOnClickListener(tabClick);
        tabSynonyms.setOnClickListener(tabClick);

        // Initialize default selection
        setSelectedTab(tabDefinition, tabDefinition, tabForms, tabSynonyms, contentDefinition, contentForms, contentSynonyms);

        return root;
    }

    private void setSelectedTab(View selected, TextView def, TextView forms, TextView syn,
                                View contentDef, View contentForms, View contentSyn) {
        def.setSelected(def == selected);
        forms.setSelected(forms == selected);
        syn.setSelected(syn == selected);

        // Adjust enabled state for accessibility
        def.setEnabled(def == selected);
        forms.setEnabled(forms == selected);
        syn.setEnabled(syn == selected);

        // Toggle content visibility
        contentDef.setVisibility(def == selected ? View.VISIBLE : View.GONE);
        contentForms.setVisibility(forms == selected ? View.VISIBLE : View.GONE);
        contentSyn.setVisibility(syn == selected ? View.VISIBLE : View.GONE);
    }
}
