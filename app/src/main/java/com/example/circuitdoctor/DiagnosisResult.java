package com.example.circuitdoctor;

import java.util.List;

public class DiagnosisResult {

    private final String title;
    private final String description;
    private final List<String> recommendations;

    public DiagnosisResult(String title, String description, List<String> recommendations) {
        this.title = title;
        this.description = description;
        this.recommendations = recommendations;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getRecommendations() { return recommendations; }
}