package com.example.circuitdoctor;

import java.util.Objects;

public class TerminalRef {

    public final long componentId;
    public final boolean isA;

    public TerminalRef(long componentId, boolean isA) {
        this.componentId = componentId;
        this.isA = isA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalRef)) return false;
        TerminalRef other = (TerminalRef) o;
        return componentId == other.componentId && isA == other.isA;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentId, isA);
    }
}