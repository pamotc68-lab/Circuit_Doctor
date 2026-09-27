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

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    private final List<MenuItem> menuItems;
    private final OnItemClickListener listener;

    public MenuAdapter(List<MenuItem> menuItems, OnItemClickListener listener) {
        this.menuItems = menuItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu_row, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        holder.bind(menuItems.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {

        private final View iconBackground;
        private final ImageView imgIcon;
        private final TextView tvTitle;
        private final TextView tvDesc;

        MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            imgIcon = itemView.findViewById(R.id.imgMenuIcon);
            tvTitle = itemView.findViewById(R.id.tvMenuTitle);
            tvDesc = itemView.findViewById(R.id.tvMenuDesc);
        }

        void bind(MenuItem item, OnItemClickListener listener) {
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