package com.example.circuitdoctor;

public class SavedCircuit {
    public final long id;
    public final String name;
    public final long savedAt;

    public SavedCircuit(long id, String name, long savedAt) {
        this.id = id;
        this.name = name;
        this.savedAt = savedAt;
    }
}