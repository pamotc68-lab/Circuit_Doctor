package com.example.circuitdoctor;

public class UsageLogEntry {
    public final String screenName;
    public final long timestamp;

    public UsageLogEntry(String screenName, long timestamp) {
        this.screenName = screenName;
        this.timestamp = timestamp;
    }
}