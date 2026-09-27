package com.example.circuitdoctor;

public class ChatMessage {
    public static final int ROLE_USER = 1;
    public static final int ROLE_AI = 2;

    public final int role;
    public final String text;

    public ChatMessage(int role, String text) {
        this.role = role;
        this.text = text;
    }
}