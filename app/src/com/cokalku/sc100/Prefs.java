package com.cokalku.sc100;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Small persistence layer for settings, memories and result history. */
public final class Prefs {
    private static final String FILE = "kalku_sc100";
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        // One-time migration: earlier builds defaulted key sound off, so users never heard it.
        if (!sp.getBoolean("soundDefault2", false)) {
            sp.edit().putBoolean("sound", true).putBoolean("soundDefault2", true).apply();
        }
    }

    public int angle() { return sp.getInt("angle", 0); }
    public void setAngle(int v) { sp.edit().putInt("angle", v).apply(); }

    public int format() { return sp.getInt("format", 0); }
    public void setFormat(int v) { sp.edit().putInt("format", v).apply(); }

    public int decimals() { return sp.getInt("decimals", 4); }
    public void setDecimals(int v) { sp.edit().putInt("decimals", v).apply(); }

    public boolean grouping() { return sp.getBoolean("grouping", true); }
    public void setGrouping(boolean v) { sp.edit().putBoolean("grouping", v).apply(); }

    public boolean haptics() { return sp.getBoolean("haptics", true); }
    public void setHaptics(boolean v) { sp.edit().putBoolean("haptics", v).apply(); }

    public boolean sound() { return sp.getBoolean("sound", true); }
    public void setSound(boolean v) { sp.edit().putBoolean("sound", v).apply(); }

    public boolean keepAwake() { return sp.getBoolean("keepAwake", true); }
    public void setKeepAwake(boolean v) { sp.edit().putBoolean("keepAwake", v).apply(); }

    public int theme() { return sp.getInt("theme", 0); }
    public void setTheme(int v) { sp.edit().putInt("theme", v).apply(); }

    public double[] memories() {
        double[] m = new double[10];
        for (int i = 0; i < 10; i++) {
            m[i] = Double.longBitsToDouble(sp.getLong("mem" + i, Double.doubleToLongBits(0)));
        }
        return m;
    }

    public void setMemory(int index, double value) {
        sp.edit().putLong("mem" + index, Double.doubleToLongBits(value)).apply();
    }

    public void clearMemories() {
        SharedPreferences.Editor e = sp.edit();
        for (int i = 0; i < 10; i++) e.putLong("mem" + i, Double.doubleToLongBits(0));
        e.apply();
    }

    public List<HistoryItem> history() {
        List<HistoryItem> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString("history", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new HistoryItem(o.optString("e"), o.optString("v"), o.optString("r", o.optString("v")), o.optLong("t")));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public void saveHistory(List<HistoryItem> list) {
        try {
            JSONArray a = new JSONArray();
            int n = Math.min(50, list.size());
            for (int i = 0; i < n; i++) {
                HistoryItem h = list.get(i);
                JSONObject o = new JSONObject();
                o.put("e", h.expression);
                o.put("v", h.value);
                o.put("r", h.raw);
                o.put("t", h.time);
                a.put(o);
            }
            sp.edit().putString("history", a.toString()).apply();
        } catch (Exception ignored) {}
    }

    public void clearHistory() { sp.edit().putString("history", "[]").apply(); }
}




