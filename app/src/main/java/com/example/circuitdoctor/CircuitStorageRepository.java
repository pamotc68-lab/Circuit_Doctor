package com.example.circuitdoctor;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CircuitStorageRepository {

    private static final String PREFS_NAME = "circuit_designer_prefs";
    private static final String KEY_CIRCUIT_IDS = "key_circuit_ids";
    private static final String KEY_NAME_PREFIX = "key_circuit_name_";
    private static final String KEY_TIME_PREFIX = "key_circuit_time_";
    private static final String KEY_COMPONENTS_PREFIX = "key_circuit_components_";
    private static final String KEY_CONNECTIONS_PREFIX = "key_circuit_connections_";

    /**
     * ข้อมูลที่บันทึกด้วยโค้ดรุ่นนี้ขึ้นต้นด้วยข้อความนี้ ส่วนข้อมูลเก่าที่เคยบันทึกไว้ (ไม่มีข้อความนี้)
     * ยังอ่านได้ตามเดิม จึงไม่ต้องลบวงจรที่บันทึกไว้ก่อนหน้า
     */
    private static final String FORMAT_V2 = "v2#";

    private final SharedPreferences prefs;

    public CircuitStorageRepository(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** บันทึกวงจรใหม่พร้อมตั้งชื่อ คืนค่า id ของวงจรที่บันทึก */
    public long saveNew(String name, List<PlacedComponent> components, List<Connection> connections) {
        List<Long> ids = getIds();

        long id = System.currentTimeMillis();
        while (ids.contains(id)) id++; // กันกรณีกดบันทึกสองครั้งในมิลลิวินาทีเดียวกัน

        ids.add(id);
        writeIds(ids);

        prefs.edit()
                .putString(KEY_NAME_PREFIX + id, name)
                .putLong(KEY_TIME_PREFIX + id, id)
                .putString(KEY_COMPONENTS_PREFIX + id, encodeComponents(components))
                .putString(KEY_CONNECTIONS_PREFIX + id, encodeConnections(connections))
                .apply();
        return id;
    }

    /** บันทึกทับวงจรเดิมที่เคยบันทึกไว้แล้ว (กรณีเปิดจากประวัติมาแก้ไข) */
    public void updateExisting(long id, List<PlacedComponent> components, List<Connection> connections) {
        prefs.edit()
                .putString(KEY_COMPONENTS_PREFIX + id, encodeComponents(components))
                .putString(KEY_CONNECTIONS_PREFIX + id, encodeConnections(connections))
                .putLong(KEY_TIME_PREFIX + id, System.currentTimeMillis())
                .apply();
    }

    public List<SavedCircuit> listAll() {
        List<SavedCircuit> result = new ArrayList<>();
        for (long id : getIds()) {
            String name = prefs.getString(KEY_NAME_PREFIX + id, "วงจรที่บันทึกไว้");
            long time = prefs.getLong(KEY_TIME_PREFIX + id, id);
            result.add(new SavedCircuit(id, name, time));
        }
        Collections.sort(result, (a, b) -> Long.compare(b.savedAt, a.savedAt));
        return result;
    }

    public CircuitState loadById(long id) {
        List<PlacedComponent> components = decodeComponents(prefs.getString(KEY_COMPONENTS_PREFIX + id, ""));
        List<Connection> connections = decodeConnections(prefs.getString(KEY_CONNECTIONS_PREFIX + id, ""));
        connections = dropDanglingConnections(components, connections);
        return new CircuitState(components, connections);
    }

    public void delete(long id) {
        List<Long> ids = getIds();
        ids.remove(Long.valueOf(id));
        writeIds(ids);

        prefs.edit()
                .remove(KEY_NAME_PREFIX + id)
                .remove(KEY_TIME_PREFIX + id)
                .remove(KEY_COMPONENTS_PREFIX + id)
                .remove(KEY_CONNECTIONS_PREFIX + id)
                .apply();
    }

    private List<Long> getIds() {
        String raw = prefs.getString(KEY_CIRCUIT_IDS, "");
        List<Long> ids = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String part : raw.split(",")) {
                try {
                    ids.add(Long.parseLong(part));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }

    private void writeIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (long id : ids) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id);
        }
        prefs.edit().putString(KEY_CIRCUIT_IDS, sb.toString()).apply();
    }

    // ===================== อุปกรณ์ =====================
    // รูปแบบ v2: "v2#" แล้วตามด้วยอุปกรณ์แต่ละชิ้นคั่นด้วย ";" โดยในแต่ละชิ้นคั่นฟิลด์ด้วย ":"
    // id:typeId:row:colA:colB:rowB:vertical:freePlaced:freeX:freeY:ledColor:customOhms:customVoltage:pressed:slideToB

    private static final int V2_COMPONENT_FIELDS = 15;

    private String encodeComponents(List<PlacedComponent> components) {
        StringBuilder sb = new StringBuilder(FORMAT_V2);
        boolean first = true;
        for (PlacedComponent p : components) {
            if (!first) sb.append(';');
            first = false;
            sb.append(p.id).append(':')
                    .append(p.typeId).append(':')
                    .append(p.row).append(':')
                    .append(p.colA).append(':')
                    .append(p.colB).append(':')
                    .append(p.rowB).append(':')
                    .append(p.vertical ? 1 : 0).append(':')
                    .append(p.freePlaced ? 1 : 0).append(':')
                    .append(p.freeX).append(':')
                    .append(p.freeY).append(':')
                    .append(p.ledColor).append(':')
                    .append(p.customOhms).append(':')
                    .append(p.customVoltage).append(':')
                    .append(p.pressed ? 1 : 0).append(':')
                    .append(p.slideToB ? 1 : 0);
        }
        return sb.toString();
    }

    private List<PlacedComponent> decodeComponents(String data) {
        List<PlacedComponent> components = new ArrayList<>();
        if (data == null || data.isEmpty()) return components;

        boolean v2 = data.startsWith(FORMAT_V2);
        String body = v2 ? data.substring(FORMAT_V2.length()) : data;
        if (body.isEmpty()) return components;

        for (String entry : body.split(";")) {
            if (entry.isEmpty()) continue;
            try {
                PlacedComponent p = v2 ? decodeComponentV2(entry) : decodeComponentLegacy(entry);
                if (p != null) components.add(p);
            } catch (RuntimeException ignored) {
                // ข้อมูลชิ้นนี้เสีย ข้ามไป แต่ยังโหลดชิ้นอื่นต่อได้ (แทนที่แอปจะปิดตัว)
            }
        }
        return components;
    }

    /** รูปแบบเก่า: id:typeId:row:colA:colB */
    private PlacedComponent decodeComponentLegacy(String entry) {
        String[] parts = entry.split(":");
        if (parts.length < 5) return null;
        long id = Long.parseLong(parts[0]);
        int typeId = Integer.parseInt(parts[1]);
        int row = Integer.parseInt(parts[2]);
        int colA = Integer.parseInt(parts[3]);
        int colB = Integer.parseInt(parts[4]);
        return new PlacedComponent(id, typeId, row, colA, colB);
    }

    private PlacedComponent decodeComponentV2(String entry) {
        String[] parts = entry.split(":");
        if (parts.length < V2_COMPONENT_FIELDS) return null;

        long id = Long.parseLong(parts[0]);
        int typeId = Integer.parseInt(parts[1]);
        int row = Integer.parseInt(parts[2]);
        int colA = Integer.parseInt(parts[3]);
        int colB = Integer.parseInt(parts[4]);

        PlacedComponent p = new PlacedComponent(id, typeId, row, colA, colB);
        p.rowB = Integer.parseInt(parts[5]);
        p.vertical = parts[6].equals("1");
        p.freePlaced = parts[7].equals("1");
        p.freeX = Float.parseFloat(parts[8]);
        p.freeY = Float.parseFloat(parts[9]);
        p.ledColor = Integer.parseInt(parts[10]);
        p.customOhms = Double.parseDouble(parts[11]);
        p.customVoltage = Double.parseDouble(parts[12]);
        p.pressed = parts[13].equals("1");
        p.slideToB = parts[14].equals("1");
        return p;
    }

    // ===================== สายไฟ =====================
    // รูปแบบ v2: "v2#" แล้วตามด้วยสายแต่ละเส้นคั่นด้วย ";" โดยสายหนึ่งเส้นคือ  จุดต้น>จุดปลาย@สี(เลขฐาน 16)
    // จุดต่อที่รูบนบอร์ด  = h<แถว>_<คอลัมน์>
    // จุดต่อที่ขาอุปกรณ์ = c<id อุปกรณ์>_<เลขขา>   (0 = A, 1 = B, 2 = ขาร่วม C ของสวิตช์เลื่อน)

    private String encodeConnections(List<Connection> connections) {
        StringBuilder sb = new StringBuilder(FORMAT_V2);
        boolean first = true;
        for (Connection c : connections) {
            if (!first) sb.append(';');
            first = false;
            sb.append(encodeTerminal(c.getFrom()))
                    .append('>')
                    .append(encodeTerminal(c.getTo()))
                    .append('@')
                    .append(Integer.toHexString(c.color));
        }
        return sb.toString();
    }

    private String encodeTerminal(TerminalRef t) {
        if (t.isHole) return "h" + t.holeRow + "_" + t.holeCol;
        return "c" + t.componentId + "_" + t.terminalIndex;
    }

    private TerminalRef decodeTerminal(String token) {
        int underscore = token.indexOf('_');
        if (token.length() < 4 || underscore < 2) {
            throw new IllegalArgumentException("bad terminal: " + token);
        }
        char kind = token.charAt(0);
        String first = token.substring(1, underscore);
        String second = token.substring(underscore + 1);
        if (kind == 'h') {
            return new TerminalRef(Integer.parseInt(first), Integer.parseInt(second));
        }
        if (kind == 'c') {
            return new TerminalRef(Long.parseLong(first), Integer.parseInt(second));
        }
        throw new IllegalArgumentException("bad terminal: " + token);
    }

    private List<Connection> decodeConnections(String data) {
        List<Connection> connections = new ArrayList<>();
        if (data == null || data.isEmpty()) return connections;

        boolean v2 = data.startsWith(FORMAT_V2);
        String body = v2 ? data.substring(FORMAT_V2.length()) : data;
        if (body.isEmpty()) return connections;

        for (String entry : body.split(";")) {
            if (entry.isEmpty()) continue;
            try {
                Connection c = v2 ? decodeConnectionV2(entry) : decodeConnectionLegacy(entry);
                if (c != null) connections.add(c);
            } catch (RuntimeException ignored) {
                // สายเส้นนี้ข้อมูลเสีย ข้ามไป สายเส้นอื่นยังโหลดได้
            }
        }
        return connections;
    }

    private Connection decodeConnectionV2(String entry) {
        int at = entry.lastIndexOf('@');
        String ends = at >= 0 ? entry.substring(0, at) : entry;
        int arrow = ends.indexOf('>');
        if (arrow < 0) return null;

        TerminalRef from = decodeTerminal(ends.substring(0, arrow));
        TerminalRef to = decodeTerminal(ends.substring(arrow + 1));

        Connection c = new Connection(from, to);
        if (at >= 0) {
            c.color = (int) Long.parseLong(entry.substring(at + 1), 16);
        }
        return c;
    }

    /** รูปแบบเก่า: fromId,fromA-toId,toA (ใช้ได้เฉพาะสายที่ต่อระหว่างขาอุปกรณ์สองขา) */
    private Connection decodeConnectionLegacy(String entry) {
        String[] sides = entry.split("-");
        if (sides.length != 2) return null;
        String[] fromParts = sides[0].split(",");
        String[] toParts = sides[1].split(",");
        if (fromParts.length != 2 || toParts.length != 2) return null;

        long fromId = Long.parseLong(fromParts[0]);
        boolean fromA = fromParts[1].equals("1");
        long toId = Long.parseLong(toParts[0]);
        boolean toA = toParts[1].equals("1");
        return new Connection(fromId, fromA, toId, toA);
    }

    /** ตัดสายที่อ้างถึงอุปกรณ์ซึ่งไม่มีอยู่จริงในวงจรที่โหลด (กันสายค้างจากข้อมูลที่เสียหาย) */
    private List<Connection> dropDanglingConnections(List<PlacedComponent> components, List<Connection> connections) {
        Set<Long> ids = new HashSet<>();
        for (PlacedComponent p : components) ids.add(p.id);

        List<Connection> kept = new ArrayList<>();
        for (Connection c : connections) {
            if (endpointExists(ids, c.getFrom()) && endpointExists(ids, c.getTo())) {
                kept.add(c);
            }
        }
        return kept;
    }

    private boolean endpointExists(Set<Long> componentIds, TerminalRef t) {
        return t.isHole || componentIds.contains(t.componentId);
    }
}