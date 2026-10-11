package com.example.circuitdoctor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChallengeRepository {

    public static final int LEVEL_EASY = 1;
    public static final int LEVEL_MEDIUM = 2;
    public static final int LEVEL_HARD = 3;

    public List<CircuitChallenge> getChallenges() {
        List<CircuitChallenge> list = new ArrayList<>();

        // ระดับง่าย
        list.add(new CircuitChallenge(1, LEVEL_EASY,
                "วงจร LED พื้นฐาน",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + ตัวต้านทาน + LED ต้องมีตัวต้านทานจำกัดกระแสเสมอ เพื่อให้ LED ติดสว่างอย่างปลอดภัย",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(2, LEVEL_EASY,
                "วงจรปุ่มกดควบคุมไฟ",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + ปุ่มกด + ตัวต้านทาน + LED เรียงต่อกันเป็นเส้นเดียว",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.PUSHBUTTON, ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(3, LEVEL_EASY,
                "วงจรพื้นฐานไม่มี LED",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + ตัวต้านทาน เท่านั้น ให้กระแสไหลครบวงจร",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.RESISTOR),
                true));

        list.add(new CircuitChallenge(10, LEVEL_EASY,
                "รู้จักตัวเก็บประจุ",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + ตัวต้านทาน + LED ให้วงจรทำงานได้ แล้ววางตัวเก็บประจุเพิ่ม 1 ตัวไว้บนบอร์ดข้างวงจร เพื่อทำความรู้จักอุปกรณ์ชนิดใหม่ (ตัวเก็บประจุมี 2 ขา เหมือนตัวต้านทาน)",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.RESISTOR, ComponentType.LED,
                        ComponentType.CAPACITOR),
                true));

        // ระดับปานกลาง
        list.add(new CircuitChallenge(4, LEVEL_MEDIUM,
                "วงจรจำกัดกระแสสองชั้น",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + ตัวต้านทาน 2 ตัว + LED 1 ดวง เรียงอนุกรมกันทั้งหมดในเส้นทางเดียว",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.RESISTOR, ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(5, LEVEL_MEDIUM,
                "วงจรสวิตช์ตัดไฟก่อนถึง LED",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + ปุ่มกด + ตัวต้านทาน + LED โดยวางปุ่มกดไว้ก่อนถึงตัวต้านทานและ LED",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.PUSHBUTTON, ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(6, LEVEL_MEDIUM,
                "วงจรเบรดบอร์ดเต็มรูปแบบ",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + เบรดบอร์ด + ตัวต้านทาน + LED โดยวางอุปกรณ์ทุกชิ้นบนเบรดบอร์ดให้ถูกต้อง",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.BREADBOARD, ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(11, LEVEL_MEDIUM,
                "สวิตช์เลื่อนเปิด-ปิดไฟ",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + สวิตช์เลื่อน + ตัวต้านทาน + LED เรียงต่อกันเป็นเส้นเดียว สวิตช์เลื่อนมี 3 ขา ให้ใช้ขากลาง (ขาร่วม) กับขาข้างใดข้างหนึ่ง แล้วแตะสวิตช์เพื่อเลื่อนไปทางขาที่ต่อวงจรไว้ ไฟจึงจะไหลครบวงจร",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.SLIDESWITCH,
                        ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(12, LEVEL_MEDIUM,
                "เบรดบอร์ดพร้อมตัวเก็บประจุ",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + เบรดบอร์ด + ตัวต้านทาน + LED + ตัวเก็บประจุ โดยวางอุปกรณ์ทุกชิ้นบนเบรดบอร์ดให้ถูกต้อง วงจรหลักคือแบตเตอรี่ → ตัวต้านทาน → LED ส่วนตัวเก็บประจุให้วางไว้บนบอร์ดข้างวงจรหลัก",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.BREADBOARD, ComponentType.RESISTOR,
                        ComponentType.LED, ComponentType.CAPACITOR),
                true));

        // ระดับยาก
        list.add(new CircuitChallenge(7, LEVEL_HARD,
                "วงจรตัวต้านทานสามตัว",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + ตัวต้านทาน 3 ตัว + LED 1 ดวง เรียงอนุกรมกันทั้งหมดในเส้นทางเดียว",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.RESISTOR, ComponentType.RESISTOR,
                        ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(8, LEVEL_HARD,
                "วงจรปลอดภัยครบองค์ประกอบ",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + ปุ่มกด + ตัวต้านทาน 2 ตัว + LED ออกแบบให้ไม่มีกระแสไหลเกินพิกัด LED",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.PUSHBUTTON, ComponentType.RESISTOR,
                        ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(9, LEVEL_HARD,
                "วงจรครบองค์ประกอบบนเบรดบอร์ด",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + เบรดบอร์ด + ปุ่มกด + ตัวต้านทาน 2 ตัว + LED โดยวางทุกชิ้นบนเบรดบอร์ดให้ถูกต้องและวงจรทำงานได้จริง",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.BREADBOARD, ComponentType.PUSHBUTTON,
                        ComponentType.RESISTOR, ComponentType.RESISTOR, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(13, LEVEL_HARD,
                "สวิตช์เลื่อนสลับไฟสองดวง",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + สวิตช์เลื่อน + ตัวต้านทาน 2 ตัว + LED 2 ดวง โดยต่อขั้วบวกแบตเตอรี่เข้าขากลางของสวิตช์ ส่วนขาข้างแต่ละขาให้ต่อผ่านตัวต้านทานและ LED ของตัวเองไปที่ขั้วลบ เมื่อแตะสวิตช์สลับตำแหน่ง ไฟจะติดทีละดวง",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.SLIDESWITCH, ComponentType.RESISTOR,
                        ComponentType.RESISTOR, ComponentType.LED, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(14, LEVEL_HARD,
                "สวิตช์เลื่อนสลับไฟบนเบรดบอร์ด",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + เบรดบอร์ด + สวิตช์เลื่อน + ตัวต้านทาน 2 ตัว + LED 2 ดวง โดยวางทุกชิ้นบนเบรดบอร์ดให้ถูกต้อง ให้สวิตช์สลับไฟระหว่าง LED ทั้งสองดวงได้ และวงจรทำงานได้จริง",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.BREADBOARD, ComponentType.SLIDESWITCH,
                        ComponentType.RESISTOR, ComponentType.RESISTOR, ComponentType.LED, ComponentType.LED),
                true));

        list.add(new CircuitChallenge(15, LEVEL_HARD,
                "วงจรครบทุกอุปกรณ์",
                "ต่อวงจรให้ครบด้วย: แบตเตอรี่ 9V + เบรดบอร์ด + ปุ่มกด + สวิตช์เลื่อน + ตัวต้านทาน 2 ตัว + LED 2 ดวง + ตัวเก็บประจุ โดยวางทุกชิ้นบนเบรดบอร์ด ใช้ปุ่มกดเป็นสวิตช์หลัก และใช้สวิตช์เลื่อนเลือกว่า LED ดวงไหนจะติด (แตะปุ่มกดและสวิตช์ให้วงจรครบก่อนกดตรวจสอบ) ส่วนตัวเก็บประจุให้วางไว้บนบอร์ดข้างวงจรหลัก",
                Arrays.asList(ComponentType.BATTERY_9V, ComponentType.BREADBOARD, ComponentType.PUSHBUTTON,
                        ComponentType.SLIDESWITCH, ComponentType.RESISTOR, ComponentType.RESISTOR,
                        ComponentType.LED, ComponentType.LED, ComponentType.CAPACITOR),
                true));

        return list;
    }
}