package com.example.circuitdoctor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ComponentPaletteAdapter extends RecyclerView.Adapter<ComponentPaletteAdapter.PaletteViewHolder> {

    public interface OnComponentPickListener {
        void onComponentPicked(ComponentType type);
    }

    private final List<ComponentType> fullList;
    private final List<ComponentType> visibleList;
    private final OnComponentPickListener listener;
    private int selectedTypeId = -1;

    public ComponentPaletteAdapter(List<ComponentType> components, OnComponentPickListener listener) {
        this.fullList = components;
        this.visibleList = new ArrayList<>(components);
        this.listener = listener;
    }

    public void filter(String query) {
        visibleList.clear();
        String q = query.trim().toLowerCase();
        for (ComponentType t : fullList) {
            if (q.isEmpty() || t.name.toLowerCase().contains(q)) {
                visibleList.add(t);
            }
        }
        notifyDataSetChanged();
    }

    public void setSelectedTypeId(int typeId) {
        this.selectedTypeId = typeId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaletteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_component_palette, parent, false);
        return new PaletteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaletteViewHolder holder, int position) {
        ComponentType type = visibleList.get(position);
        holder.bind(type, type.id == selectedTypeId, listener);
    }

    @Override
    public int getItemCount() {
        return visibleList.size();
    }

    static class PaletteViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgIcon;
        private final TextView tvName;

        PaletteViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgComponentIcon);
            tvName = itemView.findViewById(R.id.tvComponentName);
        }

        void bind(ComponentType type, boolean selected, OnComponentPickListener listener) {
            imgIcon.setImageResource(type.iconRes);
            tvName.setText(type.name);
            itemView.setAlpha(selected ? 1f : 0.85f);
            itemView.setSelected(selected);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onComponentPicked(type);
            });
        }
    }
}