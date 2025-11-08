package com.example.nt118_englishvocabapp.adapters;
//Đây là file "cầu nối" giữa RecyclerView và dữ liệu (List<Topic>).
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.Topic;

public class TopicAdapter extends ListAdapter<Topic, TopicAdapter.TopicViewHolder> {

    private final OnTopicClickListener listener;

    // Interface để xử lý sự kiện click
    public interface OnTopicClickListener {
        void onTopicClick(Topic topic);
    }

    public TopicAdapter(@NonNull OnTopicClickListener listener) {
        super(TOPIC_DIFF_CALLBACK);
        this.listener = listener;
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

    // Lớp ViewHolder
    public static class TopicViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTopicName;
        private final ImageView imgTopic;
        private final ImageView imgLockIcon;

        public TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTopicName = itemView.findViewById(R.id.txt_topic_name);
            imgTopic = itemView.findViewById(R.id.img_topic);
            imgLockIcon = itemView.findViewById(R.id.img_lock_icon);
        }

        public void bind(final Topic topic, final OnTopicClickListener listener) {
            txtTopicName.setText(topic.getTopicName());

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
            itemView.setOnClickListener(v -> listener.onTopicClick(topic));
        }
    }

    // DiffUtil để RecyclerView cập nhật hiệu quả
    private static final DiffUtil.ItemCallback<Topic> TOPIC_DIFF_CALLBACK = new DiffUtil.ItemCallback<Topic>() {
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