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

        return list;
    }
}