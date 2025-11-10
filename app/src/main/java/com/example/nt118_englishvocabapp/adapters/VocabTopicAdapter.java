package com.example.nt118_englishvocabapp.adapters;

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
import com.example.nt118_englishvocabapp.models.Topic; // 👈 DÙNG MODEL CHÍNH

import java.util.List;
import java.util.Locale;

/**
 * Adapter này dùng cho RecyclerView trong VocabFragment (Màn hình 1).
 * Nó hoạt động trực tiếp với models.Topic (data model từ API).
 * ĐÃ ĐƯỢC CẬP NHẬT ĐỂ DÙNG "item_topic_card.xml"
 */
public class VocabTopicAdapter extends ListAdapter<Topic, VocabTopicAdapter.TopicViewHolder> {

    private final OnTopicClickListener listener;
    private final SharedPreferences prefs;

    public interface OnTopicClickListener {
        void onTopicClick(Topic topic);
        void onTopicSaveClick(Topic topic, boolean isSaved);
    }

    public VocabTopicAdapter(@NonNull OnTopicClickListener listener, Context context) {
        super(TOPIC_DIFF_CALLBACK);
        this.listener = listener;
        this.prefs = context.getSharedPreferences("vocab_topic_saved_prefs", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ❗️ SỬA: Đổi tên layout thành 'item_topic_card.xml'
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_topic_card, parent, false); // 👈 ĐÃ SỬA
        return new TopicViewHolder(view, prefs);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        Topic topic = getItem(position);
        holder.bind(topic, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            Topic topic = getItem(position);
            for (Object payload : payloads) {
                if ("wordCount".equals(payload)) {
                    holder.updateWordCount(topic.getWordCount());
                }
                if ("saveState".equals(payload)) {
                    holder.updateSaveState(topic.isSaved());
                }
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getTopicId();
    }

    // Lớp ViewHolder
    public static class TopicViewHolder extends RecyclerView.ViewHolder {
        // ❗️ SỬA: Đổi tên biến để khớp với layout mới
        private final TextView txtTopic; // 👈 Sửa từ txtTopicName
        private final TextView txtDifficulty;
        private final TextView txtWords; // 👈 Sửa từ txtWordCount
        private final ImageView imgTopic;
        private final ImageButton btnSave; // 👈 Sửa từ btnSaveTopic

        // ❗️ XÓA: 'imgLockIcon' không tồn tại trong layout này
        // private final ImageView imgLockIcon;

        private final SharedPreferences prefs;

        public TopicViewHolder(@NonNull View itemView, SharedPreferences prefs) {
            super(itemView);
            this.prefs = prefs;

            // ❗️ SỬA: Ánh xạ các ID từ 'item_topic_card.xml'
            txtTopic = itemView.findViewById(R.id.txt_topic); // 👈 Sửa ID
            imgTopic = itemView.findViewById(R.id.img_topic);
            txtDifficulty = itemView.findViewById(R.id.txt_difficulty);
            txtWords = itemView.findViewById(R.id.txt_words); // 👈 Sửa ID
            btnSave = itemView.findViewById(R.id.btn_save); // 👈 Sửa ID

            // ❗️ XÓA: ID 'img_lock_icon' không tồn tại
            // imgLockIcon = itemView.findViewById(R.id.img_lock_icon);
        }

        public void bind(final Topic topic, final OnTopicClickListener listener) {
            // ❗️ SỬA: Dùng biến 'txtTopic'
            txtTopic.setText(topic.getTopicName());

            // 1. Logic Khóa (Làm mờ)
            if ("locked".equals(topic.getStatus())) {
                itemView.setAlpha(0.6f);
                // ❗️ XÓA: imgLockIcon.setVisibility(View.VISIBLE);
                if (btnSave != null) btnSave.setVisibility(View.GONE);
            } else {
                itemView.setAlpha(1.0f);
                // ❗️ XÓA: imgLockIcon.setVisibility(View.GONE);
                if (btnSave != null) btnSave.setVisibility(View.VISIBLE);
            }

            // 2. Logic Nút Lưu (Bookmark)
            String saveKey = "topic_saved_" + topic.getTopicId();
            boolean isSaved = prefs.getBoolean(saveKey, false);
            topic.setSaved(isSaved);
            updateSaveState(isSaved);

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    boolean newSavedState = !topic.isSaved();
                    topic.setSaved(newSavedState);
                    prefs.edit().putBoolean(saveKey, newSavedState).apply();
                    updateSaveState(newSavedState);
                    listener.onTopicSaveClick(topic, newSavedState);
                });
            }

            // 3. Logic Độ khó (Difficulty)
            String diff = topic.getDifficulty() != null ? topic.getDifficulty().trim().toLowerCase(Locale.ROOT) : "";
            if (diff.isEmpty()) {
                txtDifficulty.setVisibility(View.GONE);
            } else {
                txtDifficulty.setVisibility(View.VISIBLE);
                String display = Character.toUpperCase(diff.charAt(0)) + diff.substring(1);
                txtDifficulty.setText(display);

                int colorRes = R.color.text_secondary; // Fallback
                switch (diff) {
                    case "easy": colorRes = R.color.correct_green; break;
                    case "medium": colorRes = R.color.orange; break;
                    case "hard": colorRes = R.color.incorrect_red; break;
                }
                txtDifficulty.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
            }

            // 4. Logic Số lượng từ (Word Count)
            updateWordCount(topic.getWordCount());

            // 5. Logic Ảnh (Giữ nguyên)
            switch (topic.getTopicName()) {
                case "Basic Colors": imgTopic.setImageResource(R.drawable.basic_colors); break;
                case "Animals": imgTopic.setImageResource(R.drawable.animals); break;
                case "School": imgTopic.setImageResource(R.drawable.school); break;
                case "Food & Drink": imgTopic.setImageResource(R.drawable.food); break;
                case "Jobs & Workplaces": imgTopic.setImageResource(R.drawable.careers); break;
                case "Feelings & Characteristics": imgTopic.setImageResource(R.drawable.emotion); break;
                default: imgTopic.setImageResource(R.drawable.emoji_logout); // Ảnh mặc định
            }

            // 6. Logic Click Item
            itemView.setOnClickListener(v -> listener.onTopicClick(topic));
        }

        // Hàm cập nhật trạng thái Save
        void updateSaveState(boolean isSaved) {
            if (btnSave == null) return;
            int tint = isSaved ? R.color.saved_green : R.color.unsaved_gray;
            btnSave.setColorFilter(ContextCompat.getColor(itemView.getContext(), tint));
        }

        // Hàm cập nhật Số lượng từ
        void updateWordCount(int count) {
            // ❗️ SỬA: Dùng biến 'txtWords'
            if (count < 0) {
                txtWords.setText("Loading...");
            } else {
                txtWords.setText(count + " word" + (count == 1 ? "" : "s"));
            }
        }
    }

    // DiffUtil (Giữ nguyên)

    private static final DiffUtil.ItemCallback<Topic> TOPIC_DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {

        @Override

        public boolean areItemsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {

            return oldItem.getTopicId() == newItem.getTopicId();

        }



        @Override

        public boolean areContentsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {

            return oldItem.getTopicName().equals(newItem.getTopicName()) &&

                    oldItem.getStatus().equals(newItem.getStatus()) &&

                    oldItem.getWordCount() == newItem.getWordCount() &&

                    oldItem.isSaved() == newItem.isSaved(); // Thêm kiểm tra 'saved'

        }



        @Override

        public Object getChangePayload(@NonNull Topic oldItem, @NonNull Topic newItem) {

            if (oldItem.getWordCount() != newItem.getWordCount()) return "wordCount";

            if (oldItem.isSaved() != newItem.isSaved()) return "saveState";

            return null;

        }

    };

}