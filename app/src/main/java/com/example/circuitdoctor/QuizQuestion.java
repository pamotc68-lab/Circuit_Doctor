package com.example.circuitdoctor;

import java.util.List;

public class QuizQuestion {

    private final String questionText;
    private final List<QuizOption> options;

    public QuizQuestion(String questionText, List<QuizOption> options) {
        this.questionText = questionText;
        this.options = options;
    }

    public String getQuestionText() { return questionText; }
    public List<QuizOption> getOptions() { return options; }
}