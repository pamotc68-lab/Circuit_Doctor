package com.example.circuitdoctor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public final class FeatureCardViewBinder {

    private FeatureCardViewBinder() {}

    public static View inflate(ViewGroup container, FeatureCard card, OnItemClickListener listener) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.item_feature_card, container, false);

        TextView tvTitle = view.findViewById(R.id.tvFeatureTitle);
        TextView tvDesc = view.findViewById(R.id.tvFeatureDesc);
        ImageView imgIcon = view.findViewById(R.id.imgFeatureIcon);

        tvTitle.setText(card.getTitle());
        tvDesc.setText(card.getDescription());
        imgIcon.setImageResource(card.getIconRes());

        view.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(card.getType());
        });

        return view;
    }
}