package com.example.circuitdoctor;

public class LessonItem {

    private final int id;
    private final String title;
    private final String description;
    private final String levelLabel;
    private final int iconRes;
    private final int iconBackgroundColor;

    public LessonItem(int id, String title, String description, String levelLabel,
                      int iconRes, int iconBackgroundColor) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.levelLabel = levelLabel;
        this.iconRes = iconRes;
        this.iconBackgroundColor = iconBackgroundColor;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLevelLabel() { return levelLabel; }
    public int getIconRes() { return iconRes; }
    public int getIconBackgroundColor() { return iconBackgroundColor; }
}