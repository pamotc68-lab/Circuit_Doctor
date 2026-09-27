package com.example.circuitdoctor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class QuizOptionViewBinder {

    private QuizOptionViewBinder() {}

    public static View inflate(ViewGroup container, String text, int index) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.item_quiz_option, container, false);

        TextView tvOptionText = view.findViewById(R.id.tvOptionText);
        tvOptionText.setText(text);

        if (index > 0) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.topMargin = view.getResources().getDimensionPixelSize(R.dimen.card_spacing);
            view.setLayoutParams(params);
        }

        return view;
    }

    public static void setSelected(View row, boolean selected) {
        row.setBackgroundResource(selected
                ? R.drawable.bg_quiz_option_selected
                : R.drawable.bg_quiz_option_unselected);

        View radioDot = row.findViewById(R.id.radioDot);
        radioDot.setSelected(selected);
    }
}