package com.example.circuitdoctor;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UsageHistoryAdapter extends RecyclerView.Adapter<UsageHistoryAdapter.LogViewHolder> {

    private final List<UsageLogEntry> items;

    public UsageHistoryAdapter(List<UsageLogEntry> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usage_log_row, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvScreenName;
        private final TextView tvTime;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScreenName = itemView.findViewById(R.id.tvLogScreenName);
            tvTime = itemView.findViewById(R.id.tvLogTime);
        }

        void bind(UsageLogEntry entry) {
            tvScreenName.setText(entry.screenName);
            tvTime.setText(DateFormat.format("dd/MM/yyyy HH:mm", entry.timestamp));
        }
    }
}