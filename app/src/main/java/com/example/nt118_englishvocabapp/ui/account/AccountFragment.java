package com.example.nt118_englishvocabapp.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import android.transition.AutoTransition;
import android.transition.TransitionManager;

public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the existing fragment_account layout
        return inflater.inflate(com.example.nt118_englishvocabapp.R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        LinearLayout rowEdit = view.findViewById(com.example.nt118_englishvocabapp.R.id.row_edit_profile);
        final CardView expandedCard = view.findViewById(com.example.nt118_englishvocabapp.R.id.card_edit_profile_expanded);
        final ImageView chevron = view.findViewById(com.example.nt118_englishvocabapp.R.id.iv_edit_chevron);
        final View rowsContainer = view.findViewById(com.example.nt118_englishvocabapp.R.id.rows_container);

        final TextView rowLabel = view.findViewById(com.example.nt118_englishvocabapp.R.id.tv_row_edit_label);
        final TextView expandedTitle = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_card_title);

        // copy initial text
        if (rowLabel != null && expandedTitle != null) {
            expandedTitle.setText(rowLabel.getText());
        }

        if (rowEdit != null && expandedCard != null && chevron != null && rowsContainer != null) {
            rowEdit.setOnClickListener(new View.OnClickListener() {
                private boolean expanded = false;

                @Override
                public void onClick(View v) {
                    // sync title in case label changed
                    if (rowLabel != null && expandedTitle != null) {
                        expandedTitle.setText(rowLabel.getText());
                    }

                    // animate layout changes
                    TransitionManager.beginDelayedTransition((ViewGroup) rowsContainer, new AutoTransition());
                    expanded = !expanded;
                    expandedCard.setVisibility(expanded ? View.VISIBLE : View.GONE);

                    // Do NOT hide the original row label — keep it visible (user requested)
                    // if (rowLabel != null) {
                    //     rowLabel.setVisibility(expanded ? View.INVISIBLE : View.VISIBLE);
                    // }

                    // rotate chevron
                    chevron.animate().rotation(expanded ? 180f : 0f).setDuration(220).start();
                }
            });
        }
    }
}
