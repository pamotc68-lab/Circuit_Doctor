package com.example.circuitdoctor;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonViewHolder> {

    private final List<LessonItem> lessons;
    private final OnItemClickListener listener;

    public LessonAdapter(List<LessonItem> lessons, OnItemClickListener listener) {
        this.lessons = lessons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesson_row, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        holder.bind(lessons.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class LessonViewHolder extends RecyclerView.ViewHolder {

        private final View iconBackground;
        private final ImageView imgIcon;
        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvLevel;

        LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            imgIcon = itemView.findViewById(R.id.imgLessonIcon);
            tvTitle = itemView.findViewById(R.id.tvLessonTitle);
            tvDesc = itemView.findViewById(R.id.tvLessonDesc);
            tvLevel = itemView.findViewById(R.id.tvLessonLevel);
        }

        void bind(LessonItem item, OnItemClickListener listener) {
            tvTitle.setText(item.getTitle());
            tvDesc.setText(item.getDescription());
            tvLevel.setText(item.getLevelLabel());
            imgIcon.setImageResource(item.getIconRes());

            Drawable bg = iconBackground.getBackground().mutate();
            bg.setTint(item.getIconBackgroundColor());
            iconBackground.setBackground(bg);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item.getId());
            });
        }
    }
}