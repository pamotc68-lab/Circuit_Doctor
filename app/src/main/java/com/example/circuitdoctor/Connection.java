package com.example.circuitdoctor;

public class Connection {

    public final long fromId;
    public final boolean fromA;
    public final boolean fromIsHole;
    public final int fromRow;
    public final int fromCol;

    public final long toId;
    public final boolean toA;
    public final boolean toIsHole;
    public final int toRow;
    public final int toCol;

    /** สีของสายไฟเส้นนี้ (ค่าเริ่มต้น = เขียว) */
    public int color = 0xFF2E7D32;

    public Connection(long fromId, boolean fromA, long toId, boolean toA) {
        this.fromId = fromId;
        this.fromA = fromA;
        this.fromIsHole = false;
        this.fromRow = -1;
        this.fromCol = -1;

        this.toId = toId;
        this.toA = toA;
        this.toIsHole = false;
        this.toRow = -1;
        this.toCol = -1;
    }

    public Connection(TerminalRef a, TerminalRef b) {
        this.fromId = a.componentId;
        this.fromA = a.isA;
        this.fromIsHole = a.isHole;
        this.fromRow = a.holeRow;
        this.fromCol = a.holeCol;

        this.toId = b.componentId;
        this.toA = b.isA;
        this.toIsHole = b.isHole;
        this.toRow = b.holeRow;
        this.toCol = b.holeCol;
    }

    public TerminalRef getFrom() {
        return fromIsHole ? new TerminalRef(fromRow, fromCol) : new TerminalRef(fromId, fromA);
    }

    public TerminalRef getTo() {
        return toIsHole ? new TerminalRef(toRow, toCol) : new TerminalRef(toId, toA);
    }

    public boolean connects(TerminalRef a, TerminalRef b) {
        TerminalRef from = getFrom();
        TerminalRef to = getTo();
        return (from.equals(a) && to.equals(b)) || (from.equals(b) && to.equals(a));
    }

    public boolean involves(long componentId) {
        return (!fromIsHole && fromId == componentId) || (!toIsHole && toId == componentId);
    }
}