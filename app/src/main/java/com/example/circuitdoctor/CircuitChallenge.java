package com.example.circuitdoctor;

import java.util.List;

public class CircuitChallenge {
    public final int id;
    public final int level; // 1 = ง่าย, 2 = ปานกลาง, 3 = ยาก
    public final String title;
    public final String conditionText;
    public final List<Integer> requiredTypeIds; // ใส่ typeId ซ้ำได้ถ้าต้องการหลายตัว
    public final boolean requireWorkingCircuit;

    public CircuitChallenge(int id, int level, String title, String conditionText,
                            List<Integer> requiredTypeIds, boolean requireWorkingCircuit) {
        this.id = id;
        this.level = level;
        this.title = title;
        this.conditionText = conditionText;
        this.requiredTypeIds = requiredTypeIds;
        this.requireWorkingCircuit = requireWorkingCircuit;
    }
}