package com.example.nt118_englishvocabapp.adapters;
//Đây là file "cầu nối" giữa RecyclerView và dữ liệu (List<Topic>).
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.Topic;

import java.util.Locale;

public class TopicAdapter extends ListAdapter<Topic, TopicAdapter.TopicViewHolder> {

    private final OnTopicClickListener listener;

    // Interface để xử lý sự kiện click
    public interface OnTopicClickListener {
        void onTopicClick(Topic topic);
    }

    public TopicAdapter(@NonNull OnTopicClickListener listener) {
        super(TOPIC_DIFF_CALLBACK);
        this.listener = listener;
        // enable stable ids to help RecyclerView animations keep items stable
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_topic_card_keno, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        Topic topic = getItem(position);
        holder.bind(topic, listener);
    }

    // Partial bind overload: use payloads to only update changed fields (word count)
    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position, @NonNull java.util.List<Object> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        Topic topic = getItem(position);
        boolean handled = false;
        for (Object payload : payloads) {
            if (payload instanceof String && "wordCount".equals(payload)) {
                holder.updateWordCount(topic.getWordCount());
                handled = true;
            } else if (payload instanceof Integer) {
                holder.updateWordCount((Integer) payload);
                handled = true;
            }
        }
        if (!handled) {
            // fallback to full bind
            onBindViewHolder(holder, position);
        }
    }

    @Override
    public long getItemId(int position) {
        Topic t = getItem(position);
        return t != null ? t.getTopicId() : RecyclerView.NO_ID;
    }

    // Lớp ViewHolder
    public static class TopicViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTopicName;
        private final ImageView imgTopic;
        private final ImageView imgLockIcon;
        private final TextView txtDifficulty;
        private final TextView txtWordCount;
        private final ImageButton btnSave;

        public TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTopicName = itemView.findViewById(R.id.txt_topic_name);
            imgTopic = itemView.findViewById(R.id.img_topic);
            imgLockIcon = itemView.findViewById(R.id.img_lock_icon);
            // new views from item_topic_card_keno
            txtDifficulty = itemView.findViewById(R.id.txt_difficulty);
            txtWordCount = itemView.findViewById(R.id.txt_word_count);
            btnSave = itemView.findViewById(R.id.btn_save_topic);
        }

        public void bind(final Topic topic, final OnTopicClickListener listener) {
            txtTopicName.setText(topic.getTopicName());

            // Setup save button state and persistence (SharedPreferences)
            Context ctx = itemView.getContext();
            SharedPreferences prefs = ctx.getSharedPreferences("topic_saved_prefs", Context.MODE_PRIVATE);
            String key = "topic_saved_" + topic.getTopicId();
            boolean saved = prefs.getBoolean(key, topic.isSaved());
            // mirror transient model state
            topic.setSaved(saved);
            int tint = saved ? ContextCompat.getColor(ctx, R.color.saved_green)
                    : ContextCompat.getColor(ctx, R.color.unsaved_gray);
            if (btnSave != null) btnSave.setColorFilter(tint);

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    boolean newSaved = !topic.isSaved();
                    topic.setSaved(newSaved);
                    prefs.edit().putBoolean(key, newSaved).apply();
                    int newTint = newSaved ? ContextCompat.getColor(ctx, R.color.saved_green)
                            : ContextCompat.getColor(ctx, R.color.unsaved_gray);
                    btnSave.setColorFilter(newTint);
                });
            }

            // Difficulty text and color
            String diff = topic.getDifficulty() != null ? topic.getDifficulty().trim().toLowerCase(Locale.ROOT) : "";
            if (diff.isEmpty()) {
                txtDifficulty.setVisibility(View.GONE);
            } else {
                txtDifficulty.setVisibility(View.VISIBLE);
                // capitalize first letter for display
                String display = Character.toUpperCase(diff.charAt(0)) + diff.substring(1);
                txtDifficulty.setText(display);

                int colorRes;
                switch (diff) {
                    case "easy":
                        colorRes = R.color.correct_green; // green
                        break;
                    case "medium":
                        colorRes = R.color.orange; // orange
                        break;
                    case "hard":
                        colorRes = R.color.incorrect_red; // red
                        break;
                    default:
                        colorRes = R.color.text_secondary; // fallback
                }
                txtDifficulty.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
            }

            // Word count: show placeholder when unknown (-1)
            updateWordCount(topic.getWordCount());

            switch (topic.getTopicName()) {
                case "Basic Colors":
                    imgTopic.setImageResource(R.drawable.basic_colors); // Giả sử bạn có file basic_colors.png
                    break;
                case "Animals":
                    imgTopic.setImageResource(R.drawable.animals); // Giả sử bạn có file animals.png
                    break;
                case "School":
                    imgTopic.setImageResource(R.drawable.school); // Giả sử bạn có file school.png
                    break;
                case "Food & Drink":
                    imgTopic.setImageResource(R.drawable.food); // v.v...
                    break;
                case "Jobs & Workplaces":
                    imgTopic.setImageResource(R.drawable.careers);
                    break;
                case "Feelings & Characteristics":
                    imgTopic.setImageResource(R.drawable.emotion);
                    break;
                default:
                    // Đặt một ảnh mặc định nếu không khớp
                    imgTopic.setImageResource(R.drawable.emoji_logout); // Bạn cần thêm 1 ảnh tên là ic_default_image
            }

            // Logic KHÓA
            if ("locked".equals(topic.getStatus())) {
                imgLockIcon.setVisibility(View.VISIBLE);
            } else {
                imgLockIcon.setVisibility(View.GONE);
            }

            // Sự kiện Click
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTopicClick(topic);
            });

            // Note: save button already wired above with SharedPreferences persistence
        }

        // Update only the word count view to avoid full rebind flicker
        void updateWordCount(int count) {
            if (count < 0) {
                txtWordCount.setText(" ");
                txtWordCount.setVisibility(View.VISIBLE);
            } else {
                txtWordCount.setText(count + " word" + (count == 1 ? "" : "s"));
                txtWordCount.setVisibility(View.VISIBLE);
            }
        }
    }

    // DiffUtil để RecyclerView cập nhật hiệu quả
    private static final DiffUtil.ItemCallback<Topic> TOPIC_DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            return oldItem.getTopicId() == newItem.getTopicId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            return oldItem.getTopicName().equals(newItem.getTopicName()) &&
                    oldItem.getStatus().equals(newItem.getStatus()) &&
                    oldItem.getWordCount() == newItem.getWordCount();
        }

        @Override
        public Object getChangePayload(@NonNull Topic oldItem, @NonNull Topic newItem) {
            // Return a simple payload when only the wordCount changed so adapter can do partial bind
            if (oldItem.getWordCount() != newItem.getWordCount()) {
                return "wordCount";
            }
            return null; // default - full rebind
        }
    };
}