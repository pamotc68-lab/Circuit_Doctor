package com.example.circuitdoctor;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class MainRepository {

    public static final int MENU_HISTORY = 101;
    public static final int MENU_AI_CHAT = 102;
    public static final int MENU_BREADBOARD = 103;

    public List<FeatureCard> getFeatureCards(Context context) {
        List<FeatureCard> list = new ArrayList<>();
        list.add(new FeatureCard(
                FeatureCard.TYPE_WRITE_CIRCUIT,
                context.getString(R.string.feature_write_circuit_title),
                context.getString(R.string.feature_write_circuit_desc),
                R.drawable.ic_circuit
        ));
        list.add(new FeatureCard(
                FeatureCard.TYPE_QUIZ,
                context.getString(R.string.feature_quiz_title),
                context.getString(R.string.feature_quiz_desc),
                R.drawable.ic_pencil
        ));
        return list;
    }

    public List<MenuItem> getMenuItems(Context context) {
        List<MenuItem> list = new ArrayList<>();
        list.add(new MenuItem(
                MENU_HISTORY,
                context.getString(R.string.menu_history_title),
                context.getString(R.string.menu_history_desc),
                R.drawable.ic_history,
                context.getColor(R.color.accent_purple)
        ));
        list.add(new MenuItem(
                MENU_AI_CHAT,
                context.getString(R.string.menu_chat_title),
                context.getString(R.string.menu_chat_desc),
                R.drawable.ic_chat,
                context.getColor(R.color.accent_green)
        ));
        list.add(new MenuItem(
                MENU_BREADBOARD,
                context.getString(R.string.menu_breadboard_title),
                context.getString(R.string.menu_breadboard_desc),
                R.drawable.ic_breadboard,
                context.getColor(R.color.accent_pink)
        ));
        return list;
    }
}