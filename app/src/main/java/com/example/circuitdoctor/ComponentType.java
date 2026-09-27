package com.example.circuitdoctor;

public class ComponentType {

    public static final int RESISTOR = 1;
    public static final int LED = 2;
    public static final int PUSHBUTTON = 3;
    public static final int BATTERY_9V = 4;
    public static final int BREADBOARD = 5;

    public final int id;
    public final String name;
    public final int iconRes;
    public final String shortLabel;
    public final int color;

    /** ค่าตัวต้านทาน (โอห์ม) — ใช้กับ RESISTOR เท่านั้น */
    public final double resistanceOhms;
    /** แรงดันไฟจากแหล่งจ่าย (โวลต์) — ใช้กับ BATTERY_9V เท่านั้น */
    public final double sourceVoltage;
    /** แรงดันตกคร่อมเมื่อ LED ติด (โวลต์) — ใช้กับ LED เท่านั้น */
    public final double forwardVoltage;
    /** กระแสพิกัดที่ LED สว่างเต็มที่ (mA) — ใช้กับ LED เท่านั้น */
    public final double ratedCurrentMa;

    public ComponentType(int id, String name, int iconRes, String shortLabel, int color,
                         double resistanceOhms, double sourceVoltage,
                         double forwardVoltage, double ratedCurrentMa) {
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
        this.shortLabel = shortLabel;
        this.color = color;
        this.resistanceOhms = resistanceOhms;
        this.sourceVoltage = sourceVoltage;
        this.forwardVoltage = forwardVoltage;
        this.ratedCurrentMa = ratedCurrentMa;
    }
}