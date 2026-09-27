package com.example.circuitdoctor;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CircuitStorageRepository {

    private static final String PREFS_NAME = "circuit_designer_prefs";
    private static final String KEY_CIRCUIT_IDS = "key_circuit_ids";
    private static final String KEY_NAME_PREFIX = "key_circuit_name_";
    private static final String KEY_TIME_PREFIX = "key_circuit_time_";
    private static final String KEY_COMPONENTS_PREFIX = "key_circuit_components_";
    private static final String KEY_CONNECTIONS_PREFIX = "key_circuit_connections_";

    private final SharedPreferences prefs;

    public CircuitStorageRepository(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** บันทึกวงจรใหม่พร้อมตั้งชื่อ คืนค่า id ของวงจรที่บันทึก */
    public long saveNew(String name, List<PlacedComponent> components, List<Connection> connections) {
        long id = System.currentTimeMillis();
        List<Long> ids = getIds();
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
        return new CircuitState(components, connections);
    }

    public void delete(long id) {
        List<Long> ids = getIds();
        ids.remove(id);
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

    private String encodeComponents(List<PlacedComponent> components) {
        StringBuilder sb = new StringBuilder();
        for (PlacedComponent p : components) {
            if (sb.length() > 0) sb.append(";");
            sb.append(p.id).append(":").append(p.typeId).append(":")
                    .append(p.row).append(":").append(p.colA).append(":").append(p.colB);
        }
        return sb.toString();
    }

    private String encodeConnections(List<Connection> connections) {
        StringBuilder sb = new StringBuilder();
        for (Connection c : connections) {
            if (sb.length() > 0) sb.append(";");
            sb.append(c.fromId).append(",").append(c.fromA ? 1 : 0).append("-")
                    .append(c.toId).append(",").append(c.toA ? 1 : 0);
        }
        return sb.toString();
    }

    private List<PlacedComponent> decodeComponents(String data) {
        List<PlacedComponent> components = new ArrayList<>();
        if (data == null || data.isEmpty()) return components;
        for (String entry : data.split(";")) {
            String[] parts = entry.split(":");
            long id = Long.parseLong(parts[0]);
            int typeId = Integer.parseInt(parts[1]);
            int row = Integer.parseInt(parts[2]);
            int colA = Integer.parseInt(parts[3]);
            int colB = Integer.parseInt(parts[4]);
            components.add(new PlacedComponent(id, typeId, row, colA, colB));
        }
        return components;
    }

    private List<Connection> decodeConnections(String data) {
        List<Connection> connections = new ArrayList<>();
        if (data == null || data.isEmpty()) return connections;
        for (String entry : data.split(";")) {
            String[] sides = entry.split("-");
            String[] fromParts = sides[0].split(",");
            String[] toParts = sides[1].split(",");
            long fromId = Long.parseLong(fromParts[0]);
            boolean fromA = fromParts[1].equals("1");
            long toId = Long.parseLong(toParts[0]);
            boolean toA = toParts[1].equals("1");
            connections.add(new Connection(fromId, fromA, toId, toA));
        }
        return connections;
    }
}