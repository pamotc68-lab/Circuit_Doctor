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

public class SymptomAdapter extends RecyclerView.Adapter<SymptomAdapter.SymptomViewHolder> {

    private final List<SymptomItem> items;
    private final OnItemClickListener listener;

    public SymptomAdapter(List<SymptomItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SymptomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_symptom_row, parent, false);
        return new SymptomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SymptomViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SymptomViewHolder extends RecyclerView.ViewHolder {

        private final View iconBackground;
        private final ImageView imgIcon;
        private final TextView tvTitle;
        private final TextView tvDesc;

        SymptomViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            imgIcon = itemView.findViewById(R.id.imgSymptomIcon);
            tvTitle = itemView.findViewById(R.id.tvSymptomTitle);
            tvDesc = itemView.findViewById(R.id.tvSymptomDesc);
        }

        void bind(SymptomItem item, OnItemClickListener listener) {
            tvTitle.setText(item.getTitle());
            tvDesc.setText(item.getDescription());
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