package com.example.circuitdoctor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {

    public interface OnChallengeClickListener {
        void onGoToBuild(CircuitChallenge challenge);
    }

    private final List<CircuitChallenge> items;
    private final OnChallengeClickListener listener;

    public ChallengeAdapter(List<CircuitChallenge> items, OnChallengeClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge_card, parent, false);
        return new ChallengeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ChallengeViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvCondition;
        private final TextView btnGoToBuild;

        ChallengeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvChallengeTitle);
            tvCondition = itemView.findViewById(R.id.tvChallengeCondition);
            btnGoToBuild = itemView.findViewById(R.id.btnGoToBuild);
        }

        void bind(CircuitChallenge challenge, OnChallengeClickListener listener) {
            tvTitle.setText(challenge.title);
            tvCondition.setText(challenge.conditionText);
            btnGoToBuild.setOnClickListener(v -> {
                if (listener != null) listener.onGoToBuild(challenge);
            });
        }
    }
}