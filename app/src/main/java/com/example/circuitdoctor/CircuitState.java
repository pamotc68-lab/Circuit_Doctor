package com.example.circuitdoctor;

import java.util.ArrayList;
import java.util.List;

public class CircuitState {

    public final List<PlacedComponent> components;
    public final List<Connection> connections;

    public CircuitState(List<PlacedComponent> components, List<Connection> connections) {
        this.components = new ArrayList<>();
        for (PlacedComponent p : components) this.components.add(p.copy());

        this.connections = new ArrayList<>();
        for (Connection c : connections) {
            Connection copy = new Connection(c.getFrom(), c.getTo());
            copy.color = c.color;
            this.connections.add(copy);
        }
    }
}