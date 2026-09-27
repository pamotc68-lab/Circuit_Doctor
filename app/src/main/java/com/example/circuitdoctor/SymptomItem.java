package com.example.circuitdoctor;

public class SymptomItem {

    private final int id;
    private final String title;
    private final String description;
    private final int iconRes;
    private final int iconBackgroundColor;

    public SymptomItem(int id, String title, String description, int iconRes, int iconBackgroundColor) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.iconBackgroundColor = iconBackgroundColor;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconRes() { return iconRes; }
    public int getIconBackgroundColor() { return iconBackgroundColor; }
}