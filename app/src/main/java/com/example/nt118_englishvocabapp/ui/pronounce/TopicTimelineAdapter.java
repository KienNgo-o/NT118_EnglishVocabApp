package com.example.nt118_englishvocabapp.ui.pronounce;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.Topic;

import java.util.ArrayList;
import java.util.List;

public class TopicTimelineAdapter extends RecyclerView.Adapter<TopicTimelineAdapter.VH> {
    private final List<Topic> items = new ArrayList<>();
    private final OnTopicClickListener listener;

    public interface OnTopicClickListener {
        void onTopicClick(Topic topic, int position); // Updated callback to include position
    }

    public TopicTimelineAdapter(OnTopicClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Topic> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic_timeline, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Topic t = items.get(position);
        boolean left = (position % 2 == 0);
        holder.leftBox.setVisibility(left ? View.VISIBLE : View.GONE);
        holder.rightBox.setVisibility(left ? View.GONE : View.VISIBLE);

        boolean locked = "locked".equalsIgnoreCase(t.getStatus());

        if (left) {
            holder.tvTitleLeft.setText(t.getTopicName());
            int res = pickImageResForTopic(t.getTopicName());
            holder.imgLeft.setImageResource(res);
            if (holder.imgLockLeft != null) holder.imgLockLeft.setVisibility(locked ? View.VISIBLE : View.GONE);
        } else {
            holder.tvTitleRight.setText(t.getTopicName());
            int res = pickImageResForTopic(t.getTopicName());
            holder.imgRight.setImageResource(res);
            if (holder.imgLockRight != null) holder.imgLockRight.setVisibility(locked ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setAlpha(locked ? 0.6f : 1f);

        // Provide the adapter position to the listener so caller can detect the first item
        holder.itemView.setOnClickListener(v -> {
            if (!locked && listener != null) listener.onTopicClick(t, position);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private static int pickImageResForTopic(String name) {
        if (name == null) return R.drawable.emoji_logout;
        switch (name) {
            case "Basic Colors": return R.drawable.basic_colors;
            case "Animals": return R.drawable.animals;
            case "School": return R.drawable.school;
            case "Food & Drink": return R.drawable.food;
            case "Jobs & Workplaces": return R.drawable.careers;
            case "Feelings & Characteristics": return R.drawable.emotion;
            default: return R.drawable.emoji_logout;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        View leftBox, rightBox;
        TextView tvTitleLeft, tvTitleRight;
        ImageView imgLeft, imgRight;
        ImageView imgLockLeft, imgLockRight;

        VH(@NonNull View itemView) {
            super(itemView);
            leftBox = itemView.findViewById(R.id.left_box);
            rightBox = itemView.findViewById(R.id.right_box);
            tvTitleLeft = itemView.findViewById(R.id.tv_title_left);
            tvTitleRight = itemView.findViewById(R.id.tv_title_right);
            imgLeft = itemView.findViewById(R.id.img_topic_left);
            imgRight = itemView.findViewById(R.id.img_topic_right);
            imgLockLeft = itemView.findViewById(R.id.img_lock_left);
            imgLockRight = itemView.findViewById(R.id.img_lock_right);
        }
    }
}
