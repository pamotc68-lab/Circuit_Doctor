package com.example.circuitdoctor;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiagnosticRepository {

    public static final int SYMPTOM_LED_OFF = 201;
    public static final int SYMPTOM_MOTOR_NOT_WORKING = 202;
    public static final int SYMPTOM_NO_POWER = 203;
    public static final int SYMPTOM_VOLTAGE_DROP = 204;
    public static final int SYMPTOM_NO_SIGNAL = 205;

    public List<SymptomItem> getSymptomList(Context context) {
        List<SymptomItem> list = new ArrayList<>();

        list.add(new SymptomItem(
                SYMPTOM_LED_OFF,
                context.getString(R.string.symptom_led_title),
                context.getString(R.string.symptom_led_desc),
                R.drawable.ic_led,
                context.getColor(R.color.accent_blue)
        ));
        list.add(new SymptomItem(
                SYMPTOM_MOTOR_NOT_WORKING,
                context.getString(R.string.symptom_motor_title),
                context.getString(R.string.symptom_motor_desc),
                R.drawable.ic_motor,
                context.getColor(R.color.accent_pink)
        ));
        list.add(new SymptomItem(
                SYMPTOM_NO_POWER,
                context.getString(R.string.symptom_no_power_title),
                context.getString(R.string.symptom_no_power_desc),
                R.drawable.ic_power_off,
                context.getColor(R.color.accent_purple)
        ));
        list.add(new SymptomItem(
                SYMPTOM_VOLTAGE_DROP,
                context.getString(R.string.symptom_voltage_title),
                context.getString(R.string.symptom_voltage_desc),
                R.drawable.ic_voltage,
                context.getColor(R.color.accent_orange)
        ));
        list.add(new SymptomItem(
                SYMPTOM_NO_SIGNAL,
                context.getString(R.string.symptom_signal_title),
                context.getString(R.string.symptom_signal_desc),
                R.drawable.ic_signal,
                context.getColor(R.color.accent_green)
        ));

        return list;
    }

    /**
     * ประเมินผลจากจำนวนครั้งที่ผู้ใช้เลือกแต่ละกลุ่มคำตอบ (A/B/C) ใน 5 คำถาม
     * แล้วคืนคำวินิจฉัย + คำแนะนำการแก้ไขตามกลุ่มคำตอบที่ถูกเลือกมากที่สุด
     */
    public DiagnosisResult resolveResult(Context context, int symptomId,
                                         int countA, int countB, int countC) {
        int dominant = dominantCause(countA, countB, countC);

        switch (symptomId) {
            case SYMPTOM_LED_OFF:
                return ledOffResult(context, dominant);
            case SYMPTOM_MOTOR_NOT_WORKING:
                return motorResult(context, dominant);
            case SYMPTOM_NO_POWER:
                return noPowerResult(context, dominant);
            case SYMPTOM_VOLTAGE_DROP:
                return voltageDropResult(context, dominant);
            case SYMPTOM_NO_SIGNAL:
                return noSignalResult(context, dominant);
            default:
                return new DiagnosisResult("", "", new ArrayList<>());
        }
    }

    private int dominantCause(int countA, int countB, int countC) {
        if (countA >= countB && countA >= countC) return QuizOption.CAUSE_A;
        if (countB >= countA && countB >= countC) return QuizOption.CAUSE_B;
        return QuizOption.CAUSE_C;
    }

    private DiagnosisResult ledOffResult(Context c, int dominant) {
        if (dominant == QuizOption.CAUSE_A) {
            return new DiagnosisResult(
                    "วงจรไม่มีไฟจ่ายถึง LED",
                    "จากคำตอบของคุณ มีความเป็นไปได้สูงว่าวงจรไม่ได้รับไฟเลย หรือจุดต่อขาดหาย",
                    Arrays.asList(
                            "ตรวจสอบแหล่งจ่ายไฟต้นทางว่ามีแรงดันออกจริงหรือไม่",
                            "เช็คจุดเชื่อมต่อสายไฟทุกจุดว่าแน่นหนาไม่หลุด",
                            "ทดสอบวัดแรงดันไฟด้วยมัลติมิเตอร์ก่อนถึงขั้ว LED"
                    ));
        } else if (dominant == QuizOption.CAUSE_B) {
            return new DiagnosisResult(
                    "ต่อขั้ว LED ผิดด้าน หรือใส่ตัวต้านทานผิดค่า",
                    "อาการตรงกับการต่อขั้วสลับ หรือใช้ตัวต้านทานจำกัดกระแสไม่เหมาะสม",
                    Arrays.asList(
                            "ตรวจสอบขา + / - ของ LED ให้ตรงกับขั้วไฟ",
                            "คำนวณค่าตัวต้านทานใหม่ตามสูตร R = (V - Vf) / I",
                            "เปลี่ยนตัวต้านทานให้ตรงตามค่าที่คำนวณได้"
                    ));
        } else {
            return new DiagnosisResult(
                    "LED เสียหายจากการใช้งาน",
                    "จากการทดสอบเปลี่ยนดวงใหม่แล้วยังติด แสดงว่าตัว LED เดิมน่าจะเสีย",
                    Arrays.asList(
                            "เปลี่ยน LED ดวงใหม่ทดแทนดวงที่เสีย",
                            "ตรวจสอบว่ากระแสที่จ่ายไม่เกินพิกัดของ LED",
                            "หลีกเลี่ยงการจ่ายไฟเกินพิกัดในอนาคต"
                    ));
        }
    }

    private DiagnosisResult motorResult(Context c, int dominant) {
        if (dominant == QuizOption.CAUSE_A) {
            return new DiagnosisResult(
                    "มอเตอร์ไม่ได้รับไฟเลย",
                    "ไม่มีเสียงหรือแรงดันที่ขั้วมอเตอร์ แสดงว่าไฟไม่ถึงตัวมอเตอร์",
                    Arrays.asList(
                            "ตรวจสอบสายไฟและขั้วต่อไปยังมอเตอร์ทั้งหมด",
                            "เช็คแรงดันไฟจากแหล่งจ่ายก่อนถึงตัวขับมอเตอร์",
                            "ทดสอบตัวขับมอเตอร์ (Driver) ด้วยโหลดอื่นเพื่อยืนยันว่ายังทำงานได้"
                    ));
        } else if (dominant == QuizOption.CAUSE_B) {
            return new DiagnosisResult(
                    "ตัวขับมอเตอร์ทำงานผิดปกติ",
                    "มีเสียงหึ่งหรือความร้อนผิดปกติ บ่งชี้ว่า Driver/H-Bridge จ่ายกำลังไม่พอ",
                    Arrays.asList(
                            "ตรวจสอบพิกัดกระแสของตัวขับมอเตอร์ให้เหมาะกับมอเตอร์",
                            "ระบายความร้อนให้ตัวขับมอเตอร์เพียงพอ",
                            "ลองเปลี่ยนตัวขับมอเตอร์ตัวใหม่เพื่อทดสอบ"
                    ));
        } else {
            return new DiagnosisResult(
                    "มอเตอร์ติดขัดทางกล",
                    "อาการฝืดหรือติดขัดเมื่อหมุนด้วยมือ บ่งชี้ปัญหาทางกลไก ไม่ใช่ไฟฟ้า",
                    Arrays.asList(
                            "ตรวจสอบแบริ่งหรือเพลามอเตอร์ว่ามีสิ่งแปลกปลอมติดขัดหรือไม่",
                            "หยอดน้ำมันหล่อลื่นที่จุดหมุนตามความเหมาะสม",
                            "หากติดขัดถาวร ควรเปลี่ยนมอเตอร์ตัวใหม่"
                    ));
        }
    }

    private DiagnosisResult noPowerResult(Context c, int dominant) {
        if (dominant == QuizOption.CAUSE_A) {
            return new DiagnosisResult(
                    "แหล่งจ่ายไฟหรือฟิวส์เสียหาย",
                    "ไม่มีไฟออกจากแหล่งจ่าย หรือฟิวส์/เบรกเกอร์ตัดการทำงาน",
                    Arrays.asList(
                            "เปลี่ยนฟิวส์ตัวใหม่ที่มีพิกัดกระแสเท่าเดิม",
                            "รีเซ็ตเบรกเกอร์และสังเกตว่าตัดซ้ำหรือไม่",
                            "หากตัดซ้ำ ให้ตรวจหาจุดลัดวงจรก่อนใช้งานต่อ"
                    ));
        } else if (dominant == QuizOption.CAUSE_B) {
            return new DiagnosisResult(
                    "จุดต่อสายหลวมหรือเป็นสนิม",
                    "ขั้วต่อสายไฟหลักมีสนิมหรือออกไซด์เกาะ ทำให้ไฟไหลผ่านไม่สมบูรณ์",
                    Arrays.asList(
                            "ขัดทำความสะอาดจุดต่อสายด้วยกระดาษทรายละเอียด",
                            "ขันหรือบัดกรีจุดต่อให้แน่นหนา",
                            "เคลือบจุดต่อด้วยสเปรย์กันสนิมหลังทำความสะอาด"
                    ));
        } else {
            return new DiagnosisResult(
                    "สวิตช์เปิด-ปิดชำรุด",
                    "สวิตช์ทำงานไม่สม่ำเสมอ น่าจะเป็นจุดตัดต่อวงจรที่ชำรุด",
                    Arrays.asList(
                            "ทดสอบสวิตช์ด้วยมัลติมิเตอร์โหมดวัดความต่อเนื่อง",
                            "เปลี่ยนสวิตช์ตัวใหม่หากพบว่าไม่ต่อเนื่อง",
                            "ตรวจสอบสายไฟที่ต่อเข้าสวิตช์ว่าไม่หลุดหลวม"
                    ));
        }
    }

    private DiagnosisResult voltageDropResult(Context c, int dominant) {
        if (dominant == QuizOption.CAUSE_A) {
            return new DiagnosisResult(
                    "สายไฟขนาดเล็กเกินไปสำหรับโหลด",
                    "แรงดันตกมากเมื่อมีโหลด บ่งชี้ว่าสายไฟมีความต้านทานสูงเกินไป",
                    Arrays.asList(
                            "เปลี่ยนสายไฟให้มีขนาดพื้นที่หน้าตัดใหญ่ขึ้นตามกระแสที่ใช้",
                            "ลดระยะทางเดินสายไฟให้สั้นที่สุดเท่าที่ทำได้",
                            "คำนวณ Voltage Drop ตามสูตรก่อนเลือกขนาดสายใหม่"
                    ));
        } else if (dominant == QuizOption.CAUSE_B) {
            return new DiagnosisResult(
                    "ระยะเดินสายไกลเกินไป",
                    "ระยะทางจากแหล่งจ่ายถึงอุปกรณ์ไกล ทำให้เกิดความต้านทานสะสมในสาย",
                    Arrays.asList(
                            "ย้ายแหล่งจ่ายไฟให้ใกล้อุปกรณ์มากขึ้นหากทำได้",
                            "ใช้สายไฟขนาดใหญ่ขึ้นเพื่อชดเชยระยะทาง",
                            "พิจารณาติดตั้งตัวเก็บประจุ (Capacitor) ช่วยรักษาแรงดันที่ปลายทาง"
                    ));
        } else {
            return new DiagnosisResult(
                    "แบตเตอรี่หรือแหล่งจ่ายไฟเสื่อมสภาพ",
                    "แหล่งจ่ายไฟใช้งานมานานและเสื่อมสภาพ ทำให้จ่ายแรงดันได้ไม่เสถียร",
                    Arrays.asList(
                            "ทดสอบแบตเตอรี่ด้วยเครื่องวัดความจุ (Load Tester)",
                            "เปลี่ยนแบตเตอรี่หรือแหล่งจ่ายไฟตัวใหม่หากเสื่อมสภาพมาก",
                            "ตั้งรอบชาร์จ/บำรุงรักษาแหล่งจ่ายไฟอย่างสม่ำเสมอ"
                    ));
        }
    }

    private DiagnosisResult noSignalResult(Context c, int dominant) {
        if (dominant == QuizOption.CAUSE_A) {
            return new DiagnosisResult(
                    "โมดูลสัญญาณไม่ทำงาน",
                    "ไฟสถานะไม่ติดเลย บ่งชี้ว่าโมดูลสัญญาณอาจไม่ได้รับไฟหรือเสียหาย",
                    Arrays.asList(
                            "ตรวจสอบไฟเลี้ยงที่จ่ายเข้าโมดูลสัญญาณ",
                            "ทดสอบโมดูลด้วยบอร์ดอื่นเพื่อยืนยันว่าเสียหรือไม่",
                            "เปลี่ยนโมดูลสัญญาณตัวใหม่หากยืนยันว่าเสีย"
                    ));
        } else if (dominant == QuizOption.CAUSE_B) {
            return new DiagnosisResult(
                    "ต่อสายสัญญาณผิดขา หรือตั้งค่าไม่ตรงกัน",
                    "พบว่าต่อสาย Data สลับขา หรือค่า Baud Rate ไม่ตรงกันทั้งสองฝั่ง",
                    Arrays.asList(
                            "ตรวจสอบและต่อสาย TX/RX หรือ Data ให้ถูกขาตามคู่มือ",
                            "ตั้งค่า Baud Rate/ความถี่สัญญาณให้ตรงกันทั้งสองฝั่ง",
                            "รีสตาร์ทอุปกรณ์ทั้งสองฝั่งหลังแก้ไขการตั้งค่า"
                    ));
        } else {
            return new DiagnosisResult(
                    "สัญญาณอ่อนจากระยะทางหรือสิ่งกีดขวาง",
                    "ระยะห่างหรือสิ่งกีดขวางระหว่างอุปกรณ์ทำให้สัญญาณไม่เสถียร",
                    Arrays.asList(
                            "ลดระยะห่างระหว่างอุปกรณ์ส่ง-รับสัญญาณ",
                            "หลีกเลี่ยงสิ่งกีดขวางที่บดบังเส้นทางสัญญาณ",
                            "พิจารณาใช้เสาอากาศหรือตัวขยายสัญญาณเพิ่มเติม"
                    ));
        }
    }
}