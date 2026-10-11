package com.example.circuitdoctor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // ---------- ค่าคงที่ของตัวจำลองวงจร (ปรับได้ตามต้องการ) ----------
    private static final double DEFAULT_RESISTOR_OHMS = 220.0;
    private static final double DEFAULT_BATTERY_VOLTAGE = 9.0;
    private static final double MIN_RESISTOR_OHMS = 0.001;       // กันค่า 0 โอห์ม
    private static final double BATTERY_INTERNAL_OHMS = 1.0;     // ความต้านทานภายในแบตเตอรี่ (ทำให้ลัดวงจรได้กระแสสูงแต่คำนวณได้)
    private static final double LED_FORWARD_VOLTS = 2.0;         // แรงดันตกคร่อม LED ตอนติด
    private static final double LED_ON_OHMS = 10.0;              // ความต้านทานของ LED ตอนติด
    private static final double LED_FULL_BRIGHT_AMPS = 0.020;    // กระแสที่ LED สว่างเต็มที่ (20 mA)
    private static final double LED_MAX_AMPS = 0.050;            // เกินนี้ LED ไหม้ (50 mA)
    private static final double BATTERY_MAX_AMPS = 0.5;          // แบตเตอรี่จ่ายเกินนี้ถือว่าอันตราย/ลัดวงจร
    private static final double LED_LIT_MIN_AMPS = 1e-6;         // ต่ำกว่านี้ถือว่า LED ไม่ติด
    private static final double LEAK_SIEMENS = 1e-12;            // รั่วลงกราวด์เล็กน้อย กันโหนดลอยทำให้สมการแก้ไม่ได้
    private static final double SHORT_SIEMENS = 1e5;             // ใช้แทน "ลวด" ตอนคำนวณความต้านทานรวม
    private static final double EPS = 1e-9;
    private static final int MAX_LED_ITERATIONS = 60;

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

    // =====================================================================
    //  ตัวคำนวณวงจร (Nodal analysis)
    // =====================================================================

    /** กิ่งวงจรหนึ่งกิ่ง = อุปกรณ์ 1 ชิ้นที่คร่อมระหว่างโหนดไฟฟ้า a และ b */
    private static final class Branch {
        static final int RESISTOR = 0;
        static final int LED = 1;
        static final int BATTERY = 2;

        final long componentId;
        final int kind;
        final int a;
        final int b;
        final double value;   // ตัวต้านทาน = โอห์ม, แบตเตอรี่ = โวลต์

        int ledState = 0;     // LED: 0 = ดับ, 1 = นำกระแส A->B, -1 = นำกระแส B->A (LED ในแอพนี้ไม่แยกขั้ว)
        boolean active = true;
        double g = 0;         // ความนำไฟฟ้า (ซีเมนส์)
        double e = 0;         // แรงเคลื่อนไฟฟ้าอนุกรม: กระแส A->B = g * (Va - Vb - e)

        Branch(long componentId, int kind, int a, int b, double value) {
            this.componentId = componentId;
            this.kind = kind;
            this.a = a;
            this.b = b;
            this.value = value;
        }
    }

    /**
     * จำลองวงจรด้วยการวิเคราะห์แรงดันทุกโหนด จึงรองรับการต่ออนุกรม ขนาน และหลายกิ่งพร้อมกัน
     *
     * - แบตเตอรี่: แหล่งจ่ายแรงดันที่มีความต้านทานภายใน (ขั้ว B ของก้อนแรกเป็นกราวด์)
     * - ตัวต้านทาน: ตามค่าที่ตั้ง
     * - LED: นำกระแสเมื่อแรงดันคร่อมเกิน 2.0 V ไม่เช่นนั้นถือว่าขาดวงจร (ยังไม่แยกขั้ว เหมือนของเดิม)
     * - ปุ่มกดที่กดอยู่ = สายไฟ / ยังไม่กด = ขาดวงจร
     * - สวิตช์เลื่อน: ขาร่วมต่อกับขาที่เลื่อนไปหาเท่านั้น
     * - ตัวเก็บประจุ: ขาดวงจรในไฟตรง (DC) จึงไม่นำกระแสในสภาวะคงที่
     */
    public SimulationResult solve() {
        PlacedComponent firstBattery = firstOfType(ComponentType.BATTERY_9V);
        if (firstBattery == null) return SimulationResult.noBattery();

        Map<TerminalRef, TerminalRef> parent = buildElectricalGroups();

        // ---- 1) แปลงอุปกรณ์เป็นกิ่งวงจรระหว่างโหนดไฟฟ้า ----
        Map<TerminalRef, Integer> nodeIndex = new HashMap<>();
        int startNode = nodeOf(nodeIndex, find(parent, new TerminalRef(firstBattery.id, true)));
        int groundNode = nodeOf(nodeIndex, find(parent, new TerminalRef(firstBattery.id, false)));

        List<Branch> branches = new ArrayList<>();
        Branch firstBatteryBranch = null;
        for (PlacedComponent p : components) {
            int kind;
            double value = 0;
            if (p.typeId == ComponentType.RESISTOR) {
                kind = Branch.RESISTOR;
                value = Math.max(MIN_RESISTOR_OHMS, p.customOhms > 0 ? p.customOhms : DEFAULT_RESISTOR_OHMS);
            } else if (p.typeId == ComponentType.LED) {
                kind = Branch.LED;
            } else if (p.typeId == ComponentType.BATTERY_9V) {
                kind = Branch.BATTERY;
                value = p.customVoltage > 0 ? p.customVoltage : DEFAULT_BATTERY_VOLTAGE;
            } else {
                // ปุ่มกด/สวิตช์เลื่อนถูกรวมโหนดไปแล้ว, ตัวเก็บประจุขาดวงจรในไฟตรง, ชนิดอื่นไม่ใช่ชิ้นส่วนไฟฟ้า
                continue;
            }

            int a = nodeOf(nodeIndex, find(parent, new TerminalRef(p.id, true)));
            int b = nodeOf(nodeIndex, find(parent, new TerminalRef(p.id, false)));
            Branch br = new Branch(p.id, kind, a, b, value);
            branches.add(br);
            if (p.id == firstBattery.id) firstBatteryBranch = br;
        }
        int nodeCount = nodeIndex.size();

        List<List<Branch>> adjacency = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) adjacency.add(new ArrayList<>());
        for (Branch br : branches) {
            adjacency.get(br.a).add(br);
            adjacency.get(br.b).add(br);
        }

        // ---- 2) ตรวจว่ามีเส้นทางต่อครบวงจรจากขั้ว + ถึงขั้ว - ของแบตเตอรี่ก้อนแรกหรือไม่ ----
        boolean[] fromStart = reach(adjacency, startNode, firstBatteryBranch);
        if (!fromStart[groundNode]) return SimulationResult.noPath();

        // ---- 3) หาแรงดันทุกโหนด โดยวนหาสถานะ ติด/ดับ ของ LED แต่ละดวงจนนิ่ง ----
        List<Branch> leds = new ArrayList<>();
        for (Branch br : branches) {
            if (br.kind == Branch.LED) leds.add(br);
        }

        double[] volts = null;
        boolean converged = false;
        for (int iter = 0; iter < MAX_LED_ITERATIONS && !converged; iter++) {
            for (Branch br : branches) configure(br, false, firstBattery.id);
            volts = solveNodal(nodeCount, groundNode, branches, -1, 0);
            if (volts == null) break;

            converged = true;
            for (Branch led : leds) {
                double vab = volts[led.a] - volts[led.b];
                int next = led.ledState;
                if (led.ledState == 0) {
                    if (vab > LED_FORWARD_VOLTS + EPS) next = 1;
                    else if (vab < -LED_FORWARD_VOLTS - EPS) next = -1;
                } else if (led.ledState == 1) {
                    if (vab < LED_FORWARD_VOLTS - EPS) next = 0;
                } else {
                    if (vab > -LED_FORWARD_VOLTS + EPS) next = 0;
                }
                if (next != led.ledState) {
                    led.ledState = next;
                    converged = false;
                }
            }
        }
        if (volts != null && !converged) {
            // วนครบรอบแล้วยังไม่นิ่ง: ใช้ผลจากสถานะล่าสุด
            for (Branch br : branches) configure(br, false, firstBattery.id);
            volts = solveNodal(nodeCount, groundNode, branches, -1, 0);
        }
        if (volts == null) {
            // สมการแก้ไม่ได้ (แทบไม่เกิดขึ้นเพราะแบตเตอรี่มีความต้านทานภายใน) ถือว่าวงจรอันตราย
            return SimulationResult.build(false, false, 0, 0, 0, true,
                    new HashSet<>(), new HashMap<>(), new HashMap<>());
        }

        // ---- 4) สรุปผล: กระแส ความสว่าง และความอันตราย ----
        boolean danger = false;
        boolean hasResistor = false;
        double batteryAmps = 0;
        Map<Long, Double> currents = new HashMap<>();

        for (Branch br : branches) {
            double vab = volts[br.a] - volts[br.b];
            if (br.kind == Branch.BATTERY) {
                double delivered = br.g * (br.e - vab); // กระแสที่จ่ายออกจากขั้ว +
                if (br.componentId == firstBattery.id) batteryAmps = Math.max(0, delivered);
                if (Math.abs(delivered) > BATTERY_MAX_AMPS) danger = true;
                currents.put(br.componentId, Math.abs(delivered));
            } else if (br.kind == Branch.RESISTOR) {
                double amps = Math.abs(vab) / br.value;
                if (amps > EPS) hasResistor = true;
                currents.put(br.componentId, amps);
            }
        }

        boolean hasLed = false;
        double maxBrightness = 0;
        Set<Long> litIds = new HashSet<>();
        Map<Long, Double> brightnessById = new HashMap<>();

        for (Branch led : leds) {
            double vab = volts[led.a] - volts[led.b];
            double amps = led.ledState == 0 ? 0 : Math.max(0, led.ledState * led.g * (vab - led.ledState * LED_FORWARD_VOLTS));
            currents.put(led.componentId, amps);

            // LED ที่อยู่ในวงจรจริง = ทั้งสองขาต่อกลับถึงกราวด์ได้โดยไม่ต้องผ่านตัวมันเอง (ไม่ใช่ LED ที่ปลายลอย)
            boolean[] seen = reach(adjacency, groundNode, led);
            boolean inCircuit = seen[led.a] && seen[led.b];
            if (inCircuit) hasLed = true;

            double brightness = 0;
            if (inCircuit && amps > LED_MAX_AMPS) {
                danger = true; // LED ไหม้ ไม่ติด
            } else if (inCircuit && amps >= LED_LIT_MIN_AMPS) {
                brightness = Math.min(1.0, amps / LED_FULL_BRIGHT_AMPS);
                litIds.add(led.componentId);
                maxBrightness = Math.max(maxBrightness, brightness);
            }
            brightnessById.put(led.componentId, brightness);
        }

        // ---- 5) ความต้านทานรวมที่แบตเตอรี่เห็น: ถือ LED ที่ติดเป็นลวด แล้วฉีดกระแสทดสอบ 1 A ----
        double totalOhms = 0;
        if (startNode != groundNode) {
            for (Branch br : branches) configure(br, true, firstBattery.id);
            double[] rv = solveNodal(nodeCount, groundNode, branches, startNode, 1.0);
            if (rv != null) {
                double r = rv[startNode];
                if (!Double.isNaN(r) && !Double.isInfinite(r) && r >= 0 && r < 1e9) {
                    totalOhms = Math.round(r * 1000.0) / 1000.0;
                }
            }
        }

        return SimulationResult.build(hasResistor, hasLed, totalOhms, batteryAmps, maxBrightness, danger,
                litIds, brightnessById, currents);
    }

    /** ตั้งค่าความนำไฟฟ้า/แรงเคลื่อนของกิ่งตามสถานะปัจจุบัน (resistanceView = โหมดคำนวณความต้านทานรวม) */
    private void configure(Branch br, boolean resistanceView, long firstBatteryId) {
        switch (br.kind) {
            case Branch.RESISTOR:
                br.active = true;
                br.g = 1.0 / br.value;
                br.e = 0;
                break;
            case Branch.LED:
                if (br.ledState == 0) {
                    br.active = false;
                } else {
                    br.active = true;
                    if (resistanceView) {
                        br.g = SHORT_SIEMENS;
                        br.e = 0;
                    } else {
                        br.g = 1.0 / LED_ON_OHMS;
                        br.e = br.ledState * LED_FORWARD_VOLTS;
                    }
                }
                break;
            case Branch.BATTERY:
                if (resistanceView) {
                    if (br.componentId == firstBatteryId) {
                        br.active = false; // ก้อนที่กำลังวัด ถูกแทนด้วยกระแสทดสอบ
                    } else {
                        br.active = true;  // แบตเตอรี่ก้อนอื่น = ลวดเมื่อมองความต้านทาน
                        br.g = SHORT_SIEMENS;
                        br.e = 0;
                    }
                } else {
                    br.active = true;
                    br.g = 1.0 / BATTERY_INTERNAL_OHMS;
                    br.e = br.value;
                }
                break;
            default:
                br.active = false;
        }
    }

    /**
     * แก้สมการ G·V = I ของวงจร (KCL ที่ทุกโหนด) ด้วย Gaussian elimination
     * โหนด ground มีแรงดัน 0 V คืนค่าแรงดันของทุกโหนด หรือ null ถ้าแก้ไม่ได้
     * injectNode >= 0 คือโหนดที่ฉีดกระแส injectAmps เข้าไป (ใช้คำนวณความต้านทานรวม)
     */
    private double[] solveNodal(int nodeCount, int ground, List<Branch> branches, int injectNode, double injectAmps) {
        int n = nodeCount - 1;
        double[] result = new double[nodeCount];
        if (n <= 0) return result;

        double[][] m = new double[n][n + 1];
        for (int i = 0; i < nodeCount; i++) {
            int r = rowOf(i, ground);
            if (r >= 0) m[r][r] += LEAK_SIEMENS;
        }
        for (Branch br : branches) {
            if (!br.active) continue;
            int ra = rowOf(br.a, ground);
            int rb = rowOf(br.b, ground);
            if (ra >= 0) {
                m[ra][ra] += br.g;
                m[ra][n] += br.g * br.e;
            }
            if (rb >= 0) {
                m[rb][rb] += br.g;
                m[rb][n] -= br.g * br.e;
            }
            if (ra >= 0 && rb >= 0) {
                m[ra][rb] -= br.g;
                m[rb][ra] -= br.g;
            }
        }
        if (injectNode >= 0) {
            int r = rowOf(injectNode, ground);
            if (r >= 0) m[r][n] += injectAmps;
        }

        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) pivot = r;
            }
            if (Math.abs(m[pivot][col]) < 1e-30) return null;
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;

            for (int r = col + 1; r < n; r++) {
                double f = m[r][col] / m[col][col];
                if (f == 0) continue;
                for (int c = col; c <= n; c++) m[r][c] -= f * m[col][c];
            }
        }

        double[] x = new double[n];
        for (int r = n - 1; r >= 0; r--) {
            double s = m[r][n];
            for (int c = r + 1; c < n; c++) s -= m[r][c] * x[c];
            x[r] = s / m[r][r];
        }

        for (int i = 0; i < nodeCount; i++) {
            int r = rowOf(i, ground);
            result[i] = r >= 0 ? x[r] : 0;
        }
        return result;
    }

    private int rowOf(int node, int ground) {
        if (node == ground) return -1;
        return node < ground ? node : node - 1;
    }

    private int nodeOf(Map<TerminalRef, Integer> nodeIndex, TerminalRef group) {
        Integer idx = nodeIndex.get(group);
        if (idx == null) {
            idx = nodeIndex.size();
            nodeIndex.put(group, idx);
        }
        return idx;
    }

    /** หาโหนดทั้งหมดที่ไปถึงได้จากโหนด from โดยไม่ผ่านกิ่ง skip (ใส่ null ถ้าไม่ต้องข้ามกิ่งไหน) */
    private boolean[] reach(List<List<Branch>> adjacency, int from, Branch skip) {
        boolean[] seen = new boolean[adjacency.size()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seen[from] = true;
        queue.add(from);
        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (Branch br : adjacency.get(current)) {
                if (br == skip) continue;
                int other = br.a == current ? br.b : br.a;
                if (!seen[other]) {
                    seen[other] = true;
                    queue.add(other);
                }
            }
        }
        return seen;
    }

    /** รวมขาอุปกรณ์ที่ต่อถึงกันโดยตรง (สายไฟ แถบรูบนเบรดบอร์ด สวิตช์ที่ปิดอยู่) ให้เป็นโหนดไฟฟ้าเดียวกัน */
    private Map<TerminalRef, TerminalRef> buildElectricalGroups() {
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

        // สวิตช์ที่ปิดอยู่ = ขาที่เกี่ยวข้องกลายเป็นโหนดเดียวกัน
        for (PlacedComponent p : components) {
            if (p.typeId == ComponentType.PUSHBUTTON && p.pressed) {
                union(parent, new TerminalRef(p.id, true), new TerminalRef(p.id, false));
            } else if (p.typeId == ComponentType.SLIDESWITCH) {
                // สวิตช์เลื่อน 3 ทาง: ขั้วร่วม (ขา C) ต่อกับขาที่เลื่อนไปหาเท่านั้น (A หรือ B) อีกขาหนึ่งลอย (ขาดวงจร)
                union(parent, new TerminalRef(p.id, 2), new TerminalRef(p.id, p.slideToB ? 1 : 0));
            }
        }
        return parent;
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