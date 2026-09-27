package com.example.circuitdoctor;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UsageLogRepository {

    private static final String PREFS_NAME = "usage_log_prefs";
    private static final String KEY_LOG_ENTRIES = "key_log_entries";
    private static final int MAX_ENTRIES = 50;

    private final SharedPreferences prefs;

    public UsageLogRepository(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** บันทึกว่าผู้ใช้เข้าหน้าจอชื่อนี้ ณ เวลาปัจจุบัน (ใหม่สุดไว้บนสุด) */
    public void logVisit(String screenName) {
        List<String> raw = getRawEntries();
        long now = System.currentTimeMillis();
        raw.add(0, now + "|" + screenName);

        while (raw.size() > MAX_ENTRIES) {
            raw.remove(raw.size() - 1);
        }
        writeRawEntries(raw);
    }

    public List<UsageLogEntry> getRecentLogs() {
        List<UsageLogEntry> result = new ArrayList<>();
        for (String entry : getRawEntries()) {
            int sep = entry.indexOf('|');
            if (sep < 0) continue;
            try {
                long time = Long.parseLong(entry.substring(0, sep));
                String name = entry.substring(sep + 1);
                result.add(new UsageLogEntry(name, time));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public void clearAll() {
        prefs.edit().remove(KEY_LOG_ENTRIES).apply();
    }

    private List<String> getRawEntries() {
        String raw = prefs.getString(KEY_LOG_ENTRIES, "");
        List<String> list = new ArrayList<>();
        if (!raw.isEmpty()) {
            Collections.addAll(list, raw.split(";;"));
        }
        return list;
    }

    private void writeRawEntries(List<String> entries) {
        StringBuilder sb = new StringBuilder();
        for (String e : entries) {
            if (sb.length() > 0) sb.append(";;");
            sb.append(e);
        }
        prefs.edit().putString(KEY_LOG_ENTRIES, sb.toString()).apply();
    }
}