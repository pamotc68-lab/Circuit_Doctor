package com.example.circuitdoctor;

public class FeatureCard {

    public static final int TYPE_WRITE_CIRCUIT = 1;
    public static final int TYPE_QUIZ = 2;

    private final int type;
    private final String title;
    private final String description;
    private final int iconRes;

    public FeatureCard(int type, String title, String description, int iconRes) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
    }

    public int getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconRes() { return iconRes; }
}