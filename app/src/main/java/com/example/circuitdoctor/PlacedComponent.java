package com.example.circuitdoctor;

public class PlacedComponent {

    public static final int SPAN_COLUMNS = 2;
    public static final int DEFAULT_LED_COLOR = 0xFFFF1744;

    public final long id;
    public final int typeId;
    public int row;
    public int colA;
    public int colB;

    public boolean freePlaced = false;
    public float freeX = 0f;
    public float freeY = 0f;

    // ค่าที่ผู้ใช้ปรับเองได้ต่อชิ้น: -1 หมายถึง "ใช้ค่าเริ่มต้นของประเภทอุปกรณ์"
    public int ledColor = DEFAULT_LED_COLOR;
    public double customOhms = -1;
    public double customVoltage = -1;

    public PlacedComponent(long id, int typeId, int row, int colA, int colB) {
        this.id = id;
        this.typeId = typeId;
        this.row = row;
        this.colA = colA;
        this.colB = colB;
    }

    public PlacedComponent copy() {
        PlacedComponent p = new PlacedComponent(id, typeId, row, colA, colB);
        p.freePlaced = freePlaced;
        p.freeX = freeX;
        p.freeY = freeY;
        p.ledColor = ledColor;
        p.customOhms = customOhms;
        p.customVoltage = customVoltage;
        return p;
    }
}