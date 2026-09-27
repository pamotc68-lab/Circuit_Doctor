package com.example.circuitdoctor;

public class PlacedComponent {

    public static final int SPAN_COLUMNS = 2;

    public final long id;
    public final int typeId;
    public int row;
    public int colA;
    public int colB;

    public PlacedComponent(long id, int typeId, int row, int colA, int colB) {
        this.id = id;
        this.typeId = typeId;
        this.row = row;
        this.colA = colA;
        this.colB = colB;
    }

    public PlacedComponent copy() {
        return new PlacedComponent(id, typeId, row, colA, colB);
    }
}