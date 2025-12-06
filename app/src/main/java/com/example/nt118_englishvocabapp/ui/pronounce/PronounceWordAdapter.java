package com.example.nt118_englishvocabapp.ui.pronounce;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.PronounceWord;

import java.util.ArrayList;
import java.util.List;

public class PronounceWordAdapter extends RecyclerView.Adapter<PronounceWordAdapter.VH> {
    private final List<PronounceWord> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int wordId);
    }

    public PronounceWordAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PronounceWord> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pronounce_vocab, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PronounceWord w = items.get(position);
        holder.txtWord.setText(w.getWordText() != null ? w.getWordText() : "");
        holder.txtDef.setText(w.getPrimaryDefinition() != null ? w.getPrimaryDefinition() : "");
        holder.txtPhonetic.setText(w.getPhoneticSpelling() != null ? w.getPhoneticSpelling() : "");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(w.getWordId());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtWord, txtDef, txtPhonetic;
        VH(@NonNull View itemView) {
            super(itemView);
            txtWord = itemView.findViewById(R.id.txt_word);
            txtDef = itemView.findViewById(R.id.txt_definition);
            txtPhonetic = itemView.findViewById(R.id.txt_phonetic);
        }
    }
}
