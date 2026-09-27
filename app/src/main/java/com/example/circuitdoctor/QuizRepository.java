package com.example.circuitdoctor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuizRepository {

    public List<QuizQuestion> getQuestions(int symptomId) {
        switch (symptomId) {
            case DiagnosticRepository.SYMPTOM_LED_OFF:
                return ledOffQuestions();
            case DiagnosticRepository.SYMPTOM_MOTOR_NOT_WORKING:
                return motorQuestions();
            case DiagnosticRepository.SYMPTOM_NO_POWER:
                return noPowerQuestions();
            case DiagnosticRepository.SYMPTOM_VOLTAGE_DROP:
                return voltageDropQuestions();
            case DiagnosticRepository.SYMPTOM_NO_SIGNAL:
                return noSignalQuestions();
            default:
                return new ArrayList<>();
        }
    }

    private QuizQuestion q(String text, String a, String b, String c) {
        List<QuizOption> options = Arrays.asList(
                new QuizOption(a, QuizOption.CAUSE_A),
                new QuizOption(b, QuizOption.CAUSE_B),
                new QuizOption(c, QuizOption.CAUSE_C)
        );
        return new QuizQuestion(text, options);
    }

    private List<QuizQuestion> ledOffQuestions() {
        List<QuizQuestion> list = new ArrayList<>();
        list.add(q("ข้อ 1: ให้ LED ติดหรือกะพริบบ้างไหมเมื่อจ่ายไฟ?",
                "ไม่ติดเลย", "ติดๆดับๆ/กะพริบ", "ติดแสงริบหรี่กว่าปกติ"));
        list.add(q("ข้อ 2: ตรวจทิศทางขา LED (ขั้ว + / -) แล้วหรือยัง?",
                "ยังไม่ได้เช็ค", "เช็คแล้ว ถูกขั้ว", "เช็คแล้ว สลับขั้ว"));
        list.add(q("ข้อ 3: มีตัวต้านทาน (Resistor) จำกัดกระแสต่ออนุกรมกับ LED หรือไม่?",
                "ไม่มี", "มี แต่ค่าความต้านทานสูงเกินไป", "มี ค่าตามที่คำนวณไว้"));
        list.add(q("ข้อ 4: วัดแรงดันตกคร่อม LED ด้วยมัลติมิเตอร์ได้ค่าเท่าไหร่?",
                "0V (ไม่มีไฟเข้าเลย)", "ต่ำกว่าค่า Forward Voltage ปกติ", "ใกล้เคียงค่าปกติ (~2V)"));
        list.add(q("ข้อ 5: เคยลองเปลี่ยน LED ดวงใหม่ทดสอบหรือยัง?",
                "ยังไม่ได้ลอง", "ลองแล้ว ดวงใหม่ก็ไม่ติดเหมือนกัน", "ลองแล้ว ดวงใหม่ติดปกติ"));
        return list;
    }

    private List<QuizQuestion> motorQuestions() {
        List<QuizQuestion> list = new ArrayList<>();
        list.add(q("ข้อ 1: มอเตอร์มีเสียงหึ่งหรือสั่นเบาๆ ตอนจ่ายไฟหรือไม่?",
                "ไม่มีเสียงใดๆเลย", "มีเสียงหึ่งแต่ไม่หมุน", "หมุนแต่ช้ากว่าปกติมาก"));
        list.add(q("ข้อ 2: วัดแรงดันไฟที่ขั้วมอเตอร์ได้ตรงตามสเปกหรือไม่?",
                "วัดไม่ได้เลย (0V)", "วัดได้ แต่ต่ำกว่าสเปก", "วัดได้ตรงตามสเปก"));
        list.add(q("ข้อ 3: ตัวขับมอเตอร์ (Driver/H-Bridge) ร้อนผิดปกติหรือไม่?",
                "ร้อนจัดผิดปกติ", "อุ่นเล็กน้อย", "อุณหภูมิปกติ"));
        list.add(q("ข้อ 4: ลองหมุนใบพัด/เพลามอเตอร์ด้วยมือ มีอาการติดขัดหรือไม่?",
                "หมุนไม่ได้เลย ติดแน่น", "หมุนได้แต่ฝืดผิดปกติ", "หมุนลื่นปกติ"));
        list.add(q("ข้อ 5: สายไฟหรือขั้วต่อไปยังมอเตอร์อยู่ในสภาพดีหรือไม่?",
                "มีสายขาด/หลุดชัดเจน", "ขั้วต่อหลวมหรือสนิมขึ้น", "สภาพดี แน่นหนา"));
        return list;
    }

    private List<QuizQuestion> noPowerQuestions() {
        List<QuizQuestion> list = new ArrayList<>();
        list.add(q("ข้อ 1: แหล่งจ่ายไฟ (Power Supply/แบตเตอรี่) มีไฟออกหรือไม่?",
                "ไม่มีไฟออกเลย", "มีไฟออก แต่ค่าต่ำกว่าปกติ", "มีไฟออกปกติ"));
        list.add(q("ข้อ 2: ฟิวส์หรือเบรกเกอร์ในวงจรอยู่ในสภาพใด?",
                "ฟิวส์ขาด/เบรกเกอร์ตัด", "ยังไม่ได้ตรวจสอบ", "ปกติดี ไม่ขาด"));
        list.add(q("ข้อ 3: จุดเชื่อมต่อสายไฟหลัก (ขั้วบวก/ลบ) แน่นหนาดีหรือไม่?",
                "หลวมหรือหลุดชัดเจน", "แน่นแต่มีสนิม/ออกไซด์", "แน่นหนาดี"));
        list.add(q("ข้อ 4: สวิตช์เปิด-ปิดวงจรทำงานปกติหรือไม่?",
                "กดแล้วไม่มีปฏิกิริยาใดๆ", "บางครั้งติด บางครั้งไม่ติด", "ทำงานปกติ"));
        list.add(q("ข้อ 5: เคยเกิดไฟช็อตหรือกลิ่นไหม้ในวงจรหรือไม่?",
                "มีกลิ่นไหม้/รอยไหม้ชัดเจน", "มีกลิ่นแปลกๆเล็กน้อย", "ไม่มีกลิ่นผิดปกติ"));
        return list;
    }

    private List<QuizQuestion> voltageDropQuestions() {
        List<QuizQuestion> list = new ArrayList<>();
        list.add(q("ข้อ 1: แรงดันตกมากขึ้นเมื่อมีโหลดต่ออยู่หรือไม่?",
                "ตกมากเมื่อมีโหลด", "ตกเล็กน้อยเมื่อมีโหลด", "แรงดันคงที่ไม่ว่ามีโหลดหรือไม่"));
        list.add(q("ข้อ 2: สายไฟที่ใช้มีขนาดพื้นที่หน้าตัดเหมาะสมกับกระแสหรือไม่?",
                "สายเล็กเกินไปสำหรับกระแสที่ใช้", "ไม่แน่ใจ ยังไม่ได้เช็ค", "ขนาดสายเหมาะสมแล้ว"));
        list.add(q("ข้อ 3: ระยะทางเดินสายไฟจากแหล่งจ่ายถึงอุปกรณ์ไกลแค่ไหน?",
                "ไกลมาก (เกิน 5 เมตร)", "ปานกลาง", "ใกล้ (ไม่เกิน 1 เมตร)"));
        list.add(q("ข้อ 4: แบตเตอรี่หรือแหล่งจ่ายไฟเก่าหรือเสื่อมสภาพหรือไม่?",
                "ใช้งานมานานมาก/ชาร์จไม่เข้า", "ใช้งานมาสักระยะ", "ยังใหม่อยู่"));
        list.add(q("ข้อ 5: มีจุดต่อสาย (Connector) หลายจุดในเส้นทางเดินไฟหรือไม่?",
                "มีหลายจุดต่อและบางจุดหลวม", "มี 1-2 จุด แน่นหนาดี", "ต่อตรงไม่มีจุดต่อกลาง"));
        return list;
    }

    private List<QuizQuestion> noSignalQuestions() {
        List<QuizQuestion> list = new ArrayList<>();
        list.add(q("ข้อ 1: LED แสดงสถานะบนบอร์ด/โมดูลสัญญาณติดหรือไม่?",
                "ไม่ติดเลย", "ติดค้าง ไม่กะพริบ", "กะพริบปกติ"));
        list.add(q("ข้อ 2: สาย Data/สัญญาณ (TX/RX, I2C, SPI) ต่อถูกขาหรือไม่?",
                "ยังไม่ได้เช็ค", "เช็คแล้วต่อถูก", "เช็คแล้วพบว่าต่อสลับขา"));
        list.add(q("ข้อ 3: ตั้งค่า Baud Rate หรือความถี่สัญญาณตรงกันทั้งสองฝั่งหรือไม่?",
                "ไม่ตรงกัน", "ไม่แน่ใจ ยังไม่ได้ตรวจสอบ", "ตรงกันแล้ว"));
        list.add(q("ข้อ 4: ระยะห่างหรือสิ่งกีดขวางระหว่างอุปกรณ์ส่ง-รับสัญญาณเป็นอย่างไร?",
                "ไกลเกินระยะที่รองรับ/มีสิ่งกีดขวางหนา", "ระยะปานกลาง", "ระยะใกล้ ไม่มีสิ่งกีดขวาง"));
        list.add(q("ข้อ 5: ลองรีสตาร์ท/รีเซ็ตอุปกรณ์ทั้งสองฝั่งแล้วหรือยัง?",
                "ยังไม่ได้ลอง", "ลองแล้วแต่ยังไม่หาย", "ลองแล้วบางครั้งกลับมาใช้ได้"));
        return list;
    }
}