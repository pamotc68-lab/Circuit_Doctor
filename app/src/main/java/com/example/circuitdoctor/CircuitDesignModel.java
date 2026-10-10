package com.example.circuitdoctor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

public class CircuitDesignModel {

    public static final int DEFAULT_WIRE_COLOR = 0xFF2E7D32;

    public static final int[] LED_COLOR_PALETTE = {
            0xFFFF1744, // แดง
            0xFF00E676, // เขียว
            0xFF2979FF, // ฟ้า
            0xFFFFEA00, // เหลือง
            0xFFFF6D00, // ส้ม
            0xFFFFFFFF  // ขาว
    };

    private static final double DEFAULT_RESISTOR_OHMS = 220.0;
    private static final double DEFAULT_BATTERY_VOLTAGE = 9.0;

    private final List<PlacedComponent> components = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private long nextId = 1;

    public long addComponent(int typeId, int row, int colA) {
        long id = nextId++;
        int span = spanColumnsFor(typeId);
        int colB = Math.min(colA + span, BreadboardGrid.COLS - 1);
        components.add(new PlacedComponent(id, typeId, row, colA, colB));
        return id;
    }

    /** จำนวนคอลัมน์ที่อุปกรณ์แต่ละประเภทกินพื้นที่บนเบรดบอร์ด (สวิตช์เลื่อนมี 3 ขา จึงกว้างกว่าอุปกรณ์ 2 ขาทั่วไป) */
    private int spanColumnsFor(int typeId) {
        return typeId == ComponentType.SLIDESWITCH ? PlacedComponent.SWITCH_SPAN_COLUMNS : PlacedComponent.SPAN_COLUMNS;
    }

    /** วางอุปกรณ์อย่างอิสระนอกเบรดบอร์ด (ไม่ยึดรู) */
    public long addComponentFree(int typeId, float freeX, float freeY) {
        long id = nextId++;
        PlacedComponent p = new PlacedComponent(id, typeId, -1, -1, -1);
        p.freePlaced = true;
        p.freeX = freeX;
        p.freeY = freeY;
        components.add(p);
        return id;
    }

    public void moveComponent(long id, int row, int newCol) {
        for (PlacedComponent p : components) {
            if (p.id == id) {
                p.freePlaced = false;
                if (p.vertical) {
                    int span = p.rowB - p.row;
                    if (span <= 0) span = spanColumnsFor(p.typeId);
                    p.row = Math.max(0, row);
                    p.rowB = Math.min(p.row + span, BreadboardGrid.ROWS - 1);
                    p.colA = Math.max(0, Math.min(newCol, BreadboardGrid.COLS - 1));
                    p.colB = p.colA;
                } else {
                    int span = p.colB - p.colA;
                    if (span <= 0) span = spanColumnsFor(p.typeId);
                    p.row = row;
                    p.rowB = row;
                    p.colA = Math.max(0, newCol);
                    p.colB = Math.min(p.colA + span, BreadboardGrid.COLS - 1);
                }
                return;
            }
        }
    }

    /** ย้ายอุปกรณ์ไปวางอย่างอิสระนอกเบรดบอร์ด */
    public void moveComponentFree(long id, float freeX, float freeY) {
        for (PlacedComponent p : components) {
            if (p.id == id) {
                p.freePlaced = true;
                p.freeX = freeX;
                p.freeY = freeY;
                return;
            }
        }
    }

    /** สลับแนววาง (แนวนอน <-> แนวตั้ง) ของอุปกรณ์ที่วางบนบอร์ด โดยยึดตำแหน่งขา A เดิมไว้เป็นจุดหมุน */
    public void rotateOrientation(long id) {
        PlacedComponent p = findComponentById(id);
        if (p == null || p.freePlaced) return;

        if (p.vertical) {
            int span = p.rowB - p.row;
            if (span <= 0) span = spanColumnsFor(p.typeId);
            p.vertical = false;
            p.rowB = p.row;
            p.colB = Math.min(p.colA + span, BreadboardGrid.COLS - 1);
        } else {
            int span = p.colB - p.colA;
            if (span <= 0) span = spanColumnsFor(p.typeId);
            p.vertical = true;
            p.colB = p.colA;
            p.rowB = Math.min(p.row + span, BreadboardGrid.ROWS - 1);
        }
    }

    public void removeComponent(long id) {
        components.removeIf(p -> p.id == id);
        connections.removeIf(c -> c.involves(id));
    }

    public void addConnection(TerminalRef a, TerminalRef b) {
        addConnection(a, b, DEFAULT_WIRE_COLOR);
    }

    public void addConnection(TerminalRef a, TerminalRef b, int color) {
        if (a.equals(b)) return;
        for (Connection c : connections) {
            if (c.connects(a, b)) return;
        }
        Connection conn = new Connection(a, b);
        conn.color = color;
        connections.add(conn);
    }

    /** หาว่ารูที่แตะตรงกับขาของอุปกรณ์ตัวไหนหรือไม่ (เฉพาะอุปกรณ์ที่ยึดรูบนบอร์ด) */
    public TerminalRef findTerminalAt(int row, int col) {
        for (int i = components.size() - 1; i >= 0; i--) {
            PlacedComponent p = components.get(i);
            if (p.freePlaced) continue;
            if (p.vertical) {
                if (p.colA != col) continue;
                if (p.row == row) return new TerminalRef(p.id, true);
                if (p.rowB == row) return new TerminalRef(p.id, false);
                if (p.typeId == ComponentType.SLIDESWITCH && (p.row + p.rowB) / 2 == row) {
                    return new TerminalRef(p.id, 2);
                }
            } else {
                if (p.row != row) continue;
                if (p.colA == col) return new TerminalRef(p.id, true);
                if (p.colB == col) return new TerminalRef(p.id, false);
                if (p.typeId == ComponentType.SLIDESWITCH && (p.colA + p.colB) / 2 == col) {
                    return new TerminalRef(p.id, 2);
                }
            }
        }
        return null;
    }

    /** เปลี่ยนสี LED ไปสีถัดไปในชุดสี (วนกลับเมื่อครบ) */
    public void cycleLedColor(long id) {
        PlacedComponent p = findComponentById(id);
        if (p == null || p.typeId != ComponentType.LED) return;
        int idx = 0;
        for (int i = 0; i < LED_COLOR_PALETTE.length; i++) {
            if (LED_COLOR_PALETTE[i] == p.ledColor) {
                idx = i;
                break;
            }
        }
        p.ledColor = LED_COLOR_PALETTE[(idx + 1) % LED_COLOR_PALETTE.length];
    }

    /** ตั้งค่าความต้านทานเอง (โอห์ม) ให้ตัวต้านทานตัวนี้ ใส่ 0 หรือค่าติดลบเพื่อกลับไปใช้ค่าเริ่มต้น */
    public void setResistorOhms(long id, double ohms) {
        PlacedComponent p = findComponentById(id);
        if (p == null || p.typeId != ComponentType.RESISTOR) return;
        p.customOhms = ohms > 0 ? ohms : -1;
    }

    /** ตั้งค่าแรงดันเอง (โวลต์) ให้แบตเตอรี่ตัวนี้ ใส่ 0 หรือค่าติดลบเพื่อกลับไปใช้ค่าเริ่มต้น */
    public void setBatteryVoltage(long id, double volts) {
        PlacedComponent p = findComponentById(id);
        if (p == null || p.typeId != ComponentType.BATTERY_9V) return;
        p.customVoltage = volts > 0 ? volts : -1;
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
            if (p.typeId == ComponentType.SLIDESWITCH) {
                union(parent, new TerminalRef(p.id, 2), new TerminalRef(p.id, 2));
            }
        }

        // สายไฟที่ผู้ใช้ลากต่อ (ขาอุปกรณ์ หรือรูเปล่า) = ความต้านทาน 0 โอห์ม รวมกลุ่มไฟฟ้าเดียวกัน
        for (Connection c : connections) {
            union(parent, c.getFrom(), c.getTo());
        }

        // รูในแถบเดียวกันของเบรดบอร์ด (คอลัมน์เดียวกัน หรือแถบไฟบนล่าง) เชื่อมกันเองโดยไม่ต้องมีสาย
        Map<String, TerminalRef> nodeFirstSeen = new HashMap<>();
        for (PlacedComponent p : components) {
            unionByNode(parent, nodeFirstSeen, new TerminalRef(p.id, true), p.row, p.colA);
            int rowB = p.vertical ? p.rowB : p.row;
            int colB = p.vertical ? p.colA : p.colB;
            unionByNode(parent, nodeFirstSeen, new TerminalRef(p.id, false), rowB, colB);

            // ขาที่ 3 (ขั้วร่วม) ของสวิตช์เลื่อน อยู่กึ่งกลางระหว่างขา A และขา B เสมอ
            if (p.typeId == ComponentType.SLIDESWITCH) {
                int rowC = p.vertical ? (p.row + p.rowB) / 2 : p.row;
                int colC = p.vertical ? p.colA : (p.colA + p.colB) / 2;
                unionByNode(parent, nodeFirstSeen, new TerminalRef(p.id, 2), rowC, colC);
            }
        }
        // จุดต่อที่เป็นรูเปล่าจากสายไฟก็ต้องรวมเข้ากลุ่มตามแถบบอร์ดเช่นกัน
        for (Connection c : connections) {
            if (c.fromIsHole) unionByNode(parent, nodeFirstSeen, c.getFrom(), c.fromRow, c.fromCol);
            if (c.toIsHole) unionByNode(parent, nodeFirstSeen, c.getTo(), c.toRow, c.toCol);
        }

        // กราฟย่อ: โหนด = กลุ่มไฟฟ้า, เส้นเชื่อม = ตัวอุปกรณ์ที่คร่อมสองกลุ่มนั้น
        Map<TerminalRef, List<long[]>> graph = new HashMap<>();
        Map<Long, TerminalRef[]> edgeEndpoints = new HashMap<>();

        for (PlacedComponent p : components) {
            if (p.typeId == ComponentType.BATTERY_9V) continue;
            // ปุ่มกดที่ยังไม่ถูกกด ถือว่าวงจรขาด ไม่สร้างเส้นเชื่อมระหว่างสองกลุ่มไฟฟ้า
            if (p.typeId == ComponentType.PUSHBUTTON && !p.pressed) continue;

            if (p.typeId == ComponentType.SLIDESWITCH) {
                // สวิตช์เลื่อน 3 ทาง: ขั้วร่วม (ขา C) ต่อวงจรกับขาที่เลื่อนไปหาเท่านั้น (A หรือ B) ส่วนอีกขาหนึ่งถือว่าลอย (ขาดวงจร)
                TerminalRef gCom = find(parent, new TerminalRef(p.id, 2));
                TerminalRef gThrow = find(parent, new TerminalRef(p.id, p.slideToB ? 1 : 0));
                graph.computeIfAbsent(gCom, k -> new ArrayList<>()).add(new long[]{p.id});
                graph.computeIfAbsent(gThrow, k -> new ArrayList<>()).add(new long[]{p.id});
                edgeEndpoints.put(p.id, new TerminalRef[]{gCom, gThrow});
                continue;
            }

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
        Set<Long> ledIdsOnPath = new HashSet<>();

        for (long componentId : pathComponentIds) {
            PlacedComponent p = findComponentById(componentId);
            if (p == null) continue;
            if (p.typeId == ComponentType.RESISTOR) {
                hasResistor = true;
                totalResistance += (p.customOhms > 0 ? p.customOhms : DEFAULT_RESISTOR_OHMS);
            } else if (p.typeId == ComponentType.LED) {
                hasLed = true;
                ledCount++;
                ledIdsOnPath.add(componentId);
            }
        }

        double sourceVoltage = battery.customVoltage > 0 ? battery.customVoltage : DEFAULT_BATTERY_VOLTAGE;
        double forwardVoltageTotal = ledCount * 2.0;
        double effectiveVoltage = sourceVoltage - forwardVoltageTotal;

        if (hasLed && effectiveVoltage <= 0) {
            return SimulationResult.build(hasResistor, true, totalResistance, 0, 0, false, Collections.emptySet());
        }

        boolean overCurrentDanger = hasLed && !hasResistor;
        double current;
        if (totalResistance > 0) {
            current = effectiveVoltage / totalResistance;
        } else {
            current = hasLed ? 1.5 : 0;
            if (hasLed) overCurrentDanger = true;
        }

        double brightness = hasLed ? Math.min(1.0, (current * 1000.0) / 20.0) : 0;

        return SimulationResult.build(hasResistor, hasLed, totalResistance, current, brightness, overCurrentDanger, ledIdsOnPath);
    }

    private PlacedComponent firstOfType(int typeId) {
        for (PlacedComponent p : components) {
            if (p.typeId == typeId) return p;
        }
        return null;
    }

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
        if (row < 0 || col < 0) return; // อุปกรณ์วางอิสระนอกบอร์ด ไม่มีแถบไฟให้เชื่อมอัตโนมัติ
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