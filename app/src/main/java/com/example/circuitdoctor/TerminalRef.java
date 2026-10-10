package com.example.circuitdoctor;

import java.util.Objects;

public class TerminalRef {

    public final long componentId;
    /** หมายเลขขา: 0 = A, 1 = B, 2 = ขาที่ 3 (เช่น ขั้วร่วมของสวิตช์เลื่อน) */
    public final int terminalIndex;
    /** เพื่อความเข้ากันได้กับโค้ดเดิม: true เฉพาะขา A (terminalIndex == 0) เท่านั้น */
    public final boolean isA;

    public final boolean isHole;
    public final int holeRow;
    public final int holeCol;

    /** จุดต่อที่ขาอุปกรณ์ (อุปกรณ์ 2 ขาทั่วไป) */
    public TerminalRef(long componentId, boolean isA) {
        this(componentId, isA ? 0 : 1);
    }

    /** จุดต่อที่ขาอุปกรณ์ โดยระบุหมายเลขขาโดยตรง (ใช้กับอุปกรณ์ที่มีมากกว่า 2 ขา เช่น สวิตช์เลื่อน) */
    public TerminalRef(long componentId, int terminalIndex) {
        this.componentId = componentId;
        this.terminalIndex = terminalIndex;
        this.isA = terminalIndex == 0;
        this.isHole = false;
        this.holeRow = -1;
        this.holeCol = -1;
    }

    /** จุดต่อที่รูเปล่าบนเบรดบอร์ด (ไม่มีอุปกรณ์วางอยู่) */
    public TerminalRef(int holeRow, int holeCol) {
        this.componentId = -1;
        this.terminalIndex = -1;
        this.isA = false;
        this.isHole = true;
        this.holeRow = holeRow;
        this.holeCol = holeCol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalRef)) return false;
        TerminalRef other = (TerminalRef) o;
        if (isHole != other.isHole) return false;
        if (isHole) return holeRow == other.holeRow && holeCol == other.holeCol;
        return componentId == other.componentId && terminalIndex == other.terminalIndex;
    }

    @Override
    public int hashCode() {
        if (isHole) return Objects.hash(true, holeRow, holeCol);
        return Objects.hash(false, componentId, terminalIndex);
    }
}