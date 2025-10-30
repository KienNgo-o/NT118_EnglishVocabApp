// java
package com.example.nt118_englishvocabapp.ui.vocab3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.ui.vocab4.VocabFragment4;
import com.example.nt118_englishvocabapp.ui.vocab5.VocabFragment5;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

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
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_vocab3, container, false);

        // Find tab views
        TextView tabDefinition = root.findViewById(R.id.tab_definition);
        TextView tabForms = root.findViewById(R.id.tab_forms);
        TextView tabSynonyms = root.findViewById(R.id.tab_synonyms);

        // Content containers
        View contentDefinition = root.findViewById(R.id.content_definition);
        View contentForms = root.findViewById(R.id.content_forms);
        View contentSynonyms = root.findViewById(R.id.content_synonyms);

        // Lower content views to populate
        TextView wordText = root.findViewById(R.id.wordText);
        TextView wordType = root.findViewById(R.id.wordType);
        TextView definitionEn = root.findViewById(R.id.definitionEn);

        // Load args if provided
        Bundle args = getArguments();
        String word = args != null ? args.getString(ARG_WORD) : null;
        String type = args != null ? args.getString(ARG_WORD_TYPE) : null;
        String def = args != null ? args.getString(ARG_DEFINITION) : null;

        if (word != null) wordText.setText(word);
        if (type != null) wordType.setText(type);
        if (def != null) definitionEn.setText(def);

        int hostId = (container != null) ? container.getId() : android.R.id.content;
        // Helper listener to set selection or navigate
        View.OnClickListener tabClick = v -> {
            if (v == tabDefinition) {
                // show definition content inside this fragment
                setSelectedTab(v, tabDefinition, tabForms, tabSynonyms, contentDefinition, contentForms, contentSynonyms);
            } else if (v == tabForms) {
                // navigate to Forms fragment, pass args
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
                // navigate to Synonyms fragment, pass args
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

        // Initialize default selection to definition inside this fragment
        setSelectedTab(tabDefinition, tabDefinition, tabForms, tabSynonyms, contentDefinition, contentForms, contentSynonyms);

        // Bind standardized return button behavior for toolbar back
        ReturnButtonHelper.bind(root, this);

        Toast.makeText(getContext(), "Vocab Fragment 3 Opened!", Toast.LENGTH_SHORT).show();
        return root;
    }

    private void setSelectedTab(View selected, TextView def, TextView forms, TextView syn,
                                View contentDef, View contentForms, View contentSyn) {
        def.setSelected(def == selected);
        forms.setSelected(forms == selected);
        syn.setSelected(syn == selected);

        // Keep all tabs enabled so they remain clickable and can navigate between fragments.
        // Previously we disabled non-selected tabs which prevented their click listeners from firing.
        def.setEnabled(true);
        forms.setEnabled(true);
        syn.setEnabled(true);

        // Toggle content visibility
        contentDef.setVisibility(def == selected ? View.VISIBLE : View.GONE);
        contentForms.setVisibility(forms == selected ? View.VISIBLE : View.GONE);
        contentSyn.setVisibility(syn == selected ? View.VISIBLE : View.GONE);
    }
}
