package com.example.nt118_englishvocabapp.ui.quiz;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;

import java.util.List;

public class StageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_STAGE = 0;
    private static final int TYPE_EMPTY = 1;

    private final Context context;
    private final List<StageItem> items;
    private final OnStageClickListener clickListener;

    public interface OnStageClickListener {
        void onStageClick(StageItem item);
    }

    public StageAdapter(Context context, List<StageItem> items, OnStageClickListener listener) {
        this.context = context;
        this.items = items;
        this.clickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isStage() ? TYPE_STAGE : TYPE_EMPTY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_STAGE) {
            View v = inflater.inflate(R.layout.item_stage, parent, false);
            return new StageViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_empty_cell, parent, false);
            return new EmptyViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            StageItem it = items.get(position);
            if (it.isStage() && holder instanceof StageViewHolder) {
                StageViewHolder svh = (StageViewHolder) holder;
                String imageName;
                switch (it.getStageNumber()) {
                    case 1: imageName = "basic_colors"; break;
                    case 2: imageName = "animals"; break;
                    case 3: imageName = "school"; break;
                    case 4: imageName = "test"; break;
                    case 5: imageName = "emotion"; break;
                    default: imageName = "stage_" + it.getStageNumber(); break;
                }
                int drawableId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
                if (drawableId != 0) svh.stageImage.setImageResource(drawableId);
                else svh.stageImage.setImageResource(android.R.drawable.ic_menu_gallery);

                // Use custom labels for parts 1-4 where provided, otherwise fall back to "Part N"
                switch (it.getStageNumber()) {
                    case 1:
                        svh.stageLabel.setText(context.getString(R.string.basic_colors_label));
                        break;
                    case 2:
                        svh.stageLabel.setText(context.getString(R.string.animals_label));
                        break;
                    case 3:
                        svh.stageLabel.setText(context.getString(R.string.school_label));
                        break;
                    case 4:
                        svh.stageLabel.setText(context.getString(R.string.test1_label));
                        break;
                    case 5:
                        svh.stageLabel.setText(context.getString(R.string.emotion_label));
                        break;
                    default:
                        svh.stageLabel.setText(context.getString(R.string.part_label, it.getStageNumber()));
                        break;
                }
                svh.lockIcon.setVisibility(it.isUnlocked() ? View.GONE : View.VISIBLE);

                svh.itemView.setOnClickListener(v -> {
                    if (clickListener != null) clickListener.onStageClick(it);
                });
            }
        } catch (Exception e) {
            Log.e("StageAdapter", "Error binding view at pos " + position, e);
            Toast.makeText(context, "Lỗi hiển thị stage: " + e.getClass().getSimpleName() + "; xem logcat", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class StageViewHolder extends RecyclerView.ViewHolder {
        ImageView stageImage;
        TextView stageLabel;
        ImageView lockIcon;

        StageViewHolder(@NonNull View itemView) {
            super(itemView);
            stageImage = itemView.findViewById(R.id.stage_image);
            stageLabel = itemView.findViewById(R.id.stage_label);
            lockIcon = itemView.findViewById(R.id.lock_icon);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        EmptyViewHolder(@NonNull View itemView) { super(itemView); }
    }
}
