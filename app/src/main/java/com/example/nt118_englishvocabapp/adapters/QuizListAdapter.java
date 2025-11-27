package com.example.nt118_englishvocabapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.Topic;
import com.google.android.material.button.MaterialButton;

public class QuizListAdapter extends ListAdapter<Topic, QuizListAdapter.QuizViewHolder> {

    private final OnQuizStartListener listener;

    // Interface để Fragment xử lý khi bấm "Start"
    public interface OnQuizStartListener {
        void onStartQuiz(Topic topic);
    }

    public QuizListAdapter(@NonNull OnQuizStartListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Liên kết với file xml item_quiz_card mà chúng ta đã tạo
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz_card, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        Topic topic = getItem(position);
        holder.bind(topic, listener);
    }

    // --- ViewHolder ---
    static class QuizViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle, txtSubtitle, txtScoreStatus;
        private final ImageView imgTopic, imgLock;
        private final MaterialButton btnStart;
        private final Context context;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            txtTitle = itemView.findViewById(R.id.txt_quiz_title);
            txtSubtitle = itemView.findViewById(R.id.txt_quiz_subtitle);
            txtScoreStatus = itemView.findViewById(R.id.txt_score_status);
            imgTopic = itemView.findViewById(R.id.img_topic);
            imgLock = itemView.findViewById(R.id.img_lock);
            btnStart = itemView.findViewById(R.id.btn_start);
        }

        public void bind(Topic topic, OnQuizStartListener listener) {
            // 1. Đặt tên Quiz
            txtTitle.setText(topic.getTopicName() + " Quiz");

            // 2. Xử lý Hình ảnh (Copy logic từ VocabTopicAdapter)
            switch (topic.getTopicName()) {
                case "Basic Colors": imgTopic.setImageResource(R.drawable.basic_colors); break;
                case "Animals": imgTopic.setImageResource(R.drawable.animals); break;
                case "School": imgTopic.setImageResource(R.drawable.school); break;
                case "Food & Drink": imgTopic.setImageResource(R.drawable.food); break;
                case "Jobs & Workplaces": imgTopic.setImageResource(R.drawable.careers); break;
                case "Feelings & Characteristics": imgTopic.setImageResource(R.drawable.emotion); break;
                default: imgTopic.setImageResource(R.drawable.emoji_logout); // Ảnh mặc định
            }

            // 3. Xử lý Trạng thái (Locked / Unlocked / Completed)
            String status = topic.getStatus(); // "locked", "unlocked", "completed"

            if ("locked".equals(status)) {
                // --- TRƯỜNG HỢP BỊ KHÓA ---
                itemView.setAlpha(0.6f); // Làm mờ thẻ
                imgLock.setVisibility(View.VISIBLE); // Hiện ổ khóa
                btnStart.setVisibility(View.GONE); // Ẩn nút Start

                txtSubtitle.setText("Complete previous topic to unlock");
                txtScoreStatus.setVisibility(View.GONE);

                // Không cho bấm vào nút Start (dù nó ẩn)
                btnStart.setOnClickListener(null);

            } else {
                // --- TRƯỜNG HỢP MỞ KHÓA (Unlocked/Completed) ---
                itemView.setAlpha(1.0f); // Sáng rõ
                imgLock.setVisibility(View.GONE); // Ẩn ổ khóa
                btnStart.setVisibility(View.VISIBLE); // Hiện nút Start

                if ("completed".equals(status)) {
                    btnStart.setText("Retake"); // Nếu làm rồi thì hiện "Làm lại"
                    btnStart.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.correct_green)); // Đổi màu xanh
                    txtSubtitle.setText("You have passed this quiz!");
                } else {
                    btnStart.setText("Start");
                    btnStart.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.light_purple)); // Màu tím mặc định
                    txtSubtitle.setText("Test your knowledge");
                }

                // Bắt sự kiện bấm nút Start
                btnStart.setOnClickListener(v -> {
                    if (listener != null) listener.onStartQuiz(topic);
                });
            }
        }
    }

    // DiffUtil để tối ưu hiệu năng RecyclerView
    private static final DiffUtil.ItemCallback<Topic> DIFF_CALLBACK = new DiffUtil.ItemCallback<Topic>() {
        @Override
        public boolean areItemsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            return oldItem.getTopicId() == newItem.getTopicId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Topic oldItem, @NonNull Topic newItem) {
            return oldItem.getTopicName().equals(newItem.getTopicName()) &&
                    oldItem.getStatus().equals(newItem.getStatus());
        }
    };
}