package com.example.circuitdoctor;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CircuitHistoryAdapter extends RecyclerView.Adapter<CircuitHistoryAdapter.HistoryViewHolder> {

    public interface OnCircuitActionListener {
        void onOpen(SavedCircuit circuit);
        void onDelete(SavedCircuit circuit);
    }

    private final List<SavedCircuit> items;
    private final OnCircuitActionListener listener;

    public CircuitHistoryAdapter(List<SavedCircuit> items, OnCircuitActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_circuit_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvDate;
        private final TextView btnDelete;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCircuitName);
            tvDate = itemView.findViewById(R.id.tvCircuitDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteCircuit);
        }

        void bind(SavedCircuit circuit, OnCircuitActionListener listener) {
            tvName.setText(circuit.name);
            tvDate.setText(DateFormat.format("dd/MM/yyyy HH:mm", circuit.savedAt));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOpen(circuit);
            });
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(circuit);
            });
        }
    }
}