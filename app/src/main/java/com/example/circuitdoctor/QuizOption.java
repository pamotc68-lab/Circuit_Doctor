package com.example.circuitdoctor;

public class QuizOption {

    public static final int CAUSE_A = 1;
    public static final int CAUSE_B = 2;
    public static final int CAUSE_C = 3;

    private final String text;
    private final int causeCode;

    public QuizOption(String text, int causeCode) {
        this.text = text;
        this.causeCode = causeCode;
    }

    public String getText() { return text; }
    public int getCauseCode() { return causeCode; }
}