package com.example.nt118_englishvocabapp.ui.vocab2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;

import java.util.ArrayList;
import java.util.List;

public class VocabTopicAdapter extends RecyclerView.Adapter<VocabTopicAdapter.ViewHolder> {
    private List<Topic> topics = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Topic topic, int position);
    }

    public VocabTopicAdapter(List<Topic> topics, OnItemClickListener listener) {
        if (topics != null) this.topics = topics;
        this.listener = listener;
    }

    public void updateList(List<Topic> newList) {
        this.topics = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Topic t = topics.get(position);
        holder.word.setText(t.getWord());
        holder.wordType.setText(t.getWordType());
        holder.definition.setText(t.getDefinition());

        // Determine background based on position and list size
        int size = topics.size();
        if (size == 1) {
            // Single item -> round all corners
            holder.itemView.setBackgroundResource(R.drawable.bg_topic_both_rounded);
        } else if (position == 0) {
            // First item -> round top corners
            holder.itemView.setBackgroundResource(R.drawable.bg_topic_top_rounded);
        } else if (position == size - 1) {
            // Last item -> round bottom corners
            holder.itemView.setBackgroundResource(R.drawable.bg_topic_bottom_rounded);
        } else {
            // Middle items -> rectangle
            holder.itemView.setBackgroundResource(R.drawable.bg_topic_rect);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(t, position);
        });
    }

    @Override
    public int getItemCount() {
        return topics.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView word;
        TextView wordType;
        TextView definition;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            word = itemView.findViewById(R.id.txt_word);
            wordType = itemView.findViewById(R.id.txt_word_type);
            definition = itemView.findViewById(R.id.txt_definition);
        }
    }
}
