package com.example.nt118_englishvocabapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.QuizData;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class QuizOptionAdapter extends RecyclerView.Adapter<QuizOptionAdapter.OptionViewHolder> {

    private List<QuizData.Option> options = new ArrayList<>();
    private int selectedPosition = -1; // Lưu vị trí đang chọn (-1 là chưa chọn gì)
    private OnOptionSelectedListener listener;

    public interface OnOptionSelectedListener {
        void onOptionSelected(int optionId);
    }

    // Cập nhật danh sách đáp án mới
    public void setOptions(List<QuizData.Option> options, OnOptionSelectedListener listener) {
        this.options = options;
        this.listener = listener;
        this.selectedPosition = -1; // Reset lại khi qua câu hỏi mới
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        QuizData.Option option = options.get(position);
        boolean isSelected = position == selectedPosition;

        // Hiệu ứng chọn
        if (isSelected) {
            holder.cardView.setStrokeWidth(6); // Viền dày lên
            holder.cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.purple_700));
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.light_purple));
        } else {
            holder.cardView.setStrokeWidth(0);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
        }

        // Hiển thị nội dung (Hình hoặc Chữ)
        if (option.optionImageUrl != null && !option.optionImageUrl.isEmpty()) {
            holder.txtOption.setVisibility(View.GONE);
            holder.imgOption.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(option.optionImageUrl).into(holder.imgOption);
        } else {
            holder.txtOption.setVisibility(View.VISIBLE);
            holder.imgOption.setVisibility(View.GONE);
            holder.txtOption.setText(option.optionText);
        }

        // Click Event
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Chỉ cập nhật 2 item bị thay đổi để tối ưu hiệu năng
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onOptionSelected(option.optionId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    // --- CÁC HÀM BẠN CẦN THÊM VÀO ĐÂY ---

    // 1. Lấy ID của đáp án đang chọn (để gửi lên server)
    public int getSelectedOptionId() {
        if (selectedPosition != -1 && selectedPosition < options.size()) {
            return options.get(selectedPosition).optionId;
        }
        return -1; // Chưa chọn gì
    }

    // 2. Xóa lựa chọn (khi chuyển sang câu hỏi mới)
    public void clearSelection() {
        selectedPosition = -1;
        notifyDataSetChanged();
    }
    // ------------------------------------

    static class OptionViewHolder extends RecyclerView.ViewHolder {
        TextView txtOption;
        ImageView imgOption;
        MaterialCardView cardView;

        public OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOption = itemView.findViewById(R.id.txt_option);
            imgOption = itemView.findViewById(R.id.img_option);
            cardView = itemView.findViewById(R.id.card_option);
        }
    }
}