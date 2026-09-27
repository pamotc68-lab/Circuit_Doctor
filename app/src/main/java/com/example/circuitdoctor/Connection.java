package com.example.circuitdoctor;

public class Connection {

    public final long fromId;
    public final boolean fromA;
    public final long toId;
    public final boolean toA;

    public Connection(long fromId, boolean fromA, long toId, boolean toA) {
        this.fromId = fromId;
        this.fromA = fromA;
        this.toId = toId;
        this.toA = toA;
    }

    public boolean connects(TerminalRef a, TerminalRef b) {
        TerminalRef from = new TerminalRef(fromId, fromA);
        TerminalRef to = new TerminalRef(toId, toA);
        return (from.equals(a) && to.equals(b)) || (from.equals(b) && to.equals(a));
    }

    public boolean involves(long componentId) {
        return fromId == componentId || toId == componentId;
    }
}