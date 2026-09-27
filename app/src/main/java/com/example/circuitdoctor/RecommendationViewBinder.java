package com.example.circuitdoctor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class RecommendationViewBinder {

    private RecommendationViewBinder() {}

    public static View inflate(ViewGroup container, int stepNumber, String text) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.item_recommendation, container, false);

        TextView tvNumber = view.findViewById(R.id.tvStepNumber);
        TextView tvText = view.findViewById(R.id.tvStepText);

        tvNumber.setText(String.valueOf(stepNumber));
        tvText.setText(text);

        return view;
    }
}