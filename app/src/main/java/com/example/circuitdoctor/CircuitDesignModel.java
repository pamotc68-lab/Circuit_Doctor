package com.example.circuitdoctor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CircuitDesignModel {

    private final List<PlacedComponent> components = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private long nextId = 1;

    public long addComponent(int typeId, int row, int colA) {
        long id = nextId++;
        int colB = Math.min(colA + PlacedComponent.SPAN_COLUMNS, BreadboardGrid.COLS - 1);
        components.add(new PlacedComponent(id, typeId, row, colA, colB));
        return id;
    }

    public void moveComponent(long id, int row, int newColA) {
        for (PlacedComponent p : components) {
            if (p.id == id) {
                int span = p.colB - p.colA;
                p.row = row;
                p.colA = Math.max(0, newColA);
                p.colB = Math.min(p.colA + span, BreadboardGrid.COLS - 1);
                return;
            }
        }
    }

    public void removeComponent(long id) {
        components.removeIf(p -> p.id == id);
        connections.removeIf(c -> c.involves(id));
    }

    public void addConnection(TerminalRef a, TerminalRef b) {
        if (a.equals(b)) return;
        for (Connection c : connections) {
            if (c.connects(a, b)) return;
        }
        connections.add(new Connection(a.componentId, a.isA, b.componentId, b.isA));
    }

    /** หาว่ารูที่แตะตรงกับขา A หรือ B ของอุปกรณ์ตัวไหนหรือไม่ */
    public TerminalRef findTerminalAt(int row, int col) {
        for (int i = components.size() - 1; i >= 0; i--) {
            PlacedComponent p = components.get(i);
            if (p.row != row) continue;
            if (p.colA == col) return new TerminalRef(p.id, true);
            if (p.colB == col) return new TerminalRef(p.id, false);
        }
        return null;
    }

    public PlacedComponent findComponentById(long id) {
        for (PlacedComponent p : components) {
            if (p.id == id) return p;
        }
        return null;
    }

    public List<PlacedComponent> getComponents() { return components; }
    public List<Connection> getConnections() { return connections; }

    public CircuitState snapshot() { return new CircuitState(components, connections); }

    public void restore(CircuitState state) {
        components.clear();
        components.addAll(state.components);
        connections.clear();
        connections.addAll(state.connections);

        long max = 0;
        for (PlacedComponent p : components) max = Math.max(max, p.id);
        nextId = max + 1;
    }

    /**
     * คำนวณวงจรจริงตามกฎของโอห์ม (V = I × R) สำหรับวงจรอนุกรมเส้นเดียว:
     * รวมค่าตัวต้านทานตลอดเส้นทาง หักแรงดัน Forward Voltage ของ LED แล้วหากระแสที่ไหลจริง
     */
    public SimulationResult solve() {
        PlacedComponent battery = firstOfType(ComponentType.BATTERY_9V);
        if (battery == null) return SimulationResult.noBattery();

        Map<TerminalRef, TerminalRef> parent = new HashMap<>();
        for (PlacedComponent p : components) {
            union(parent, new TerminalRef(p.id, true), new TerminalRef(p.id, true));
            union(parent, new TerminalRef(p.id, false), new TerminalRef(p.id, false));
        }

        // สายไฟที่ผู้ใช้ลากต่อ = ความต้านทาน 0 โอห์ม รวมกลุ่มไฟฟ้าเดียวกัน
        for (Connection c : connections) {
            union(parent, new TerminalRef(c.fromId, c.fromA), new TerminalRef(c.toId, c.toA));
        }

        // รูในแถบเดียวกันของเบรดบอร์ด (คอลัมน์เดียวกัน หรือแถบไฟบนล่าง) เชื่อมกันเองโดยไม่ต้องมีสาย
        Map<String, TerminalRef> nodeFirstSeen = new HashMap<>();
        for (PlacedComponent p : components) {
            unionByNode(parent, nodeFirstSeen, new TerminalRef(p.id, true), p.row, p.colA);
            unionByNode(parent, nodeFirstSeen, new TerminalRef(p.id, false), p.row, p.colB);
        }

        // กราฟย่อ: โหนด = กลุ่มไฟฟ้า, เส้นเชื่อม = ตัวอุปกรณ์ (resistor/LED/pushbutton) ที่คร่อมสองกลุ่มนั้น
        Map<TerminalRef, List<long[]>> graph = new HashMap<>(); // key: group root, value: [otherGroupHash-not-used, componentId]
        Map<TerminalRef, TerminalRef> groupNeighbor = new HashMap<>();
        List<long[]> edgeList = new ArrayList<>(); // [componentId] with resolved endpoints stored separately
        Map<Long, TerminalRef[]> edgeEndpoints = new HashMap<>();

        for (PlacedComponent p : components) {
            if (p.typeId == ComponentType.BATTERY_9V) continue;
            TerminalRef gA = find(parent, new TerminalRef(p.id, true));
            TerminalRef gB = find(parent, new TerminalRef(p.id, false));
            graph.computeIfAbsent(gA, k -> new ArrayList<>()).add(new long[]{p.id});
            graph.computeIfAbsent(gB, k -> new ArrayList<>()).add(new long[]{p.id});
            edgeEndpoints.put(p.id, new TerminalRef[]{gA, gB});
        }

        TerminalRef startGroup = find(parent, new TerminalRef(battery.id, true));
        TerminalRef endGroup = find(parent, new TerminalRef(battery.id, false));

        // BFS หาเส้นทางจากขั้ว + ถึงขั้ว - ของแบตเตอรี่ ผ่านกลุ่มไฟฟ้า
        Map<TerminalRef, TerminalRef> cameFromGroup = new HashMap<>();
        Map<TerminalRef, Long> cameFromComponent = new HashMap<>();
        ArrayDeque<TerminalRef> queue = new ArrayDeque<>();
        queue.add(startGroup);
        cameFromGroup.put(startGroup, startGroup);

        while (!queue.isEmpty()) {
            TerminalRef current = queue.poll();
            if (current.equals(endGroup)) break;

            for (long[] edge : graph.getOrDefault(current, new ArrayList<>())) {
                long componentId = edge[0];
                TerminalRef[] ends = edgeEndpoints.get(componentId);
                TerminalRef other = ends[0].equals(current) ? ends[1] : ends[0];
                if (!cameFromGroup.containsKey(other)) {
                    cameFromGroup.put(other, current);
                    cameFromComponent.put(other, componentId);
                    queue.add(other);
                }
            }
        }

        if (!cameFromGroup.containsKey(endGroup)) {
            return SimulationResult.noPath();
        }

        // ย้อนเส้นทางกลับเพื่อรวบรวมอุปกรณ์ที่อยู่บนเส้นทาง
        List<Long> pathComponentIds = new ArrayList<>();
        TerminalRef walker = endGroup;
        while (!walker.equals(startGroup)) {
            pathComponentIds.add(cameFromComponent.get(walker));
            walker = cameFromGroup.get(walker);
        }

        double totalResistance = 0;
        boolean hasResistor = false;
        boolean hasLed = false;
        int ledCount = 0;

        for (long componentId : pathComponentIds) {
            PlacedComponent p = findComponentById(componentId);
            if (p == null) continue;
            if (p.typeId == ComponentType.RESISTOR) {
                hasResistor = true;
                totalResistance += 220.0;
            } else if (p.typeId == ComponentType.LED) {
                hasLed = true;
                ledCount++;
            }
        }

        double sourceVoltage = 9.0;
        double forwardVoltageTotal = ledCount * 2.0;
        double effectiveVoltage = sourceVoltage - forwardVoltageTotal;

        if (hasLed && effectiveVoltage <= 0) {
            return SimulationResult.build(hasResistor, true, totalResistance, 0, 0, false);
        }

        boolean overCurrentDanger = hasLed && !hasResistor;
        double current;
        if (totalResistance > 0) {
            current = effectiveVoltage / totalResistance;
        } else {
            current = hasLed ? 1.5 : 0; // ไม่มีตัวต้านทานจำกัดกระแส = กระแสไหลเกินพิกัดทันที
            if (hasLed) overCurrentDanger = true;
        }

        double brightness = hasLed ? Math.min(1.0, (current * 1000.0) / 20.0) : 0;

        return SimulationResult.build(hasResistor, hasLed, totalResistance, current, brightness, overCurrentDanger);
    }

    private PlacedComponent firstOfType(int typeId) {
        for (PlacedComponent p : components) {
            if (p.typeId == typeId) return p;
        }
        return null;
    }

    // --- Union-Find ช่วยจัดกลุ่มโหนดไฟฟ้าที่มีความต้านทาน 0 โอห์ม ---

    private TerminalRef find(Map<TerminalRef, TerminalRef> parent, TerminalRef t) {
        TerminalRef p = parent.get(t);
        if (p == null) return t;
        if (p.equals(t)) return t;
        TerminalRef root = find(parent, p);
        parent.put(t, root);
        return root;
    }

    private void union(Map<TerminalRef, TerminalRef> parent, TerminalRef a, TerminalRef b) {
        TerminalRef rootA = find(parent, a);
        TerminalRef rootB = find(parent, b);
        if (!rootA.equals(rootB)) parent.put(rootA, rootB);
    }

    private void unionByNode(Map<TerminalRef, TerminalRef> parent, Map<String, TerminalRef> nodeFirstSeen,
                             TerminalRef terminal, int row, int col) {
        String nodeId = BreadboardGrid.nodeIdFor(row, col);
        if (nodeId == null) return;
        TerminalRef existing = nodeFirstSeen.get(nodeId);
        if (existing == null) {
            nodeFirstSeen.put(nodeId, terminal);
        } else {
            union(parent, existing, terminal);
        }
    }
}