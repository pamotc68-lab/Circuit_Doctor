package com.example.circuitdoctor;

public class PlacedComponent {

    public static final int SPAN_COLUMNS = 2;
    /** สวิตช์เลื่อนมี 3 ขา จึงกินพื้นที่กว้างกว่าอุปกรณ์ 2 ขาทั่วไป */
    public static final int SWITCH_SPAN_COLUMNS = 4;
    public static final int DEFAULT_LED_COLOR = 0xFFFF1744;

    public final long id;
    public final int typeId;
    public int row;
    public int colA;
    public int colB;

    /** ใช้เมื่อวางแนวตั้ง (vertical == true): rowB คือแถวของขา B (ตอนนั้น colB จะเท่ากับ colA เสมอ) */
    public int rowB;
    public boolean vertical = false;

    public boolean freePlaced = false;
    public float freeX = 0f;
    public float freeY = 0f;

    // ค่าที่ผู้ใช้ปรับเองได้ต่อชิ้น: -1 หมายถึง "ใช้ค่าเริ่มต้นของประเภทอุปกรณ์"
    public int ledColor = DEFAULT_LED_COLOR;
    public double customOhms = -1;
    public double customVoltage = -1;

    // สถานะปุ่มกด: true = กดเปิดสวิตช์อยู่ (ต่อวงจร), false = ยังไม่กด (วงจรขาด)
    public boolean pressed = false;

    // สถานะสวิตช์เลื่อน (SLIDESWITCH เท่านั้น): false = เลื่อนไปทางขา A, true = เลื่อนไปทางขา B
    public boolean slideToB = false;

    public PlacedComponent(long id, int typeId, int row, int colA, int colB) {
        this.id = id;
        this.typeId = typeId;
        this.row = row;
        this.colA = colA;
        this.colB = colB;
        this.rowB = row;
    }

    public PlacedComponent copy() {
        PlacedComponent p = new PlacedComponent(id, typeId, row, colA, colB);
        p.rowB = rowB;
        p.vertical = vertical;
        p.freePlaced = freePlaced;
        p.freeX = freeX;
        p.freeY = freeY;
        p.ledColor = ledColor;
        p.customOhms = customOhms;
        p.customVoltage = customVoltage;
        p.pressed = pressed;
        p.slideToB = slideToB;
        return p;
    }
}