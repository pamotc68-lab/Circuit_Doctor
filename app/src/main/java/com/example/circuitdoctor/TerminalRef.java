package com.example.circuitdoctor;

import java.util.Objects;

public class TerminalRef {

    public final long componentId;
    public final boolean isA;

    public final boolean isHole;
    public final int holeRow;
    public final int holeCol;

    /** จุดต่อที่ขาอุปกรณ์ */
    public TerminalRef(long componentId, boolean isA) {
        this.componentId = componentId;
        this.isA = isA;
        this.isHole = false;
        this.holeRow = -1;
        this.holeCol = -1;
    }

    /** จุดต่อที่รูเปล่าบนเบรดบอร์ด (ไม่มีอุปกรณ์วางอยู่) */
    public TerminalRef(int holeRow, int holeCol) {
        this.componentId = -1;
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
        return componentId == other.componentId && isA == other.isA;
    }

    @Override
    public int hashCode() {
        if (isHole) return Objects.hash(true, holeRow, holeCol);
        return Objects.hash(false, componentId, isA);
    }
}