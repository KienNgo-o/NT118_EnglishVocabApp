package com.example.nt118_englishvocabapp.ui.vocab;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;

import java.util.ArrayList;
import java.util.List;

public class TopicCardAdapter extends RecyclerView.Adapter<TopicCardAdapter.ViewHolder> {
    private List<TopicCard> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TopicCard item, int position);
    }

    public TopicCardAdapter(List<TopicCard> items, OnItemClickListener listener) {
        if (items != null) this.items = items;
        this.listener = listener;
    }

    public void updateList(List<TopicCard> newList) {
        this.items = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TopicCard t = items.get(position);
        holder.txtTopic.setText(t.title);
        holder.txtDifficulty.setText(t.difficulty);
        holder.txtWords.setText(t.wordsCount + " words");
        holder.imgTopic.setImageResource(t.imageResId);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(t, position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgTopic;
        TextView txtDifficulty;
        TextView txtTopic;
        TextView txtWords;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgTopic = itemView.findViewById(R.id.img_topic);
            txtDifficulty = itemView.findViewById(R.id.txt_difficulty);
            txtTopic = itemView.findViewById(R.id.txt_topic);
            txtWords = itemView.findViewById(R.id.txt_words);
        }
    }
}

