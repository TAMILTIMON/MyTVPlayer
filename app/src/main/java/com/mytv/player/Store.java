package com.mytv.player;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Keeps playlists and resume positions on the TV. */
public class Store {
    private static SharedPreferences prefs;
    private static SharedPreferences posPrefs;
    private static final LinkedHashMap<String, List<String[]>> data = new LinkedHashMap<>();
    public static volatile int version = 0;

    public static synchronized void init(Context c) {
        if (prefs != null) return;
        Context app = c.getApplicationContext();
        prefs = app.getSharedPreferences("mytv", Context.MODE_PRIVATE);
        posPrefs = app.getSharedPreferences("mytv_pos", Context.MODE_PRIVATE);
        load();
    }

    private static void load() {
        data.clear();
        try {
            JSONObject o = new JSONObject(prefs.getString("data", "{\"playlists\":[]}"));
            JSONArray a = o.getJSONArray("playlists");
            for (int i = 0; i < a.length(); i++) {
                JSONObject p = a.getJSONObject(i);
                JSONArray it = p.getJSONArray("items");
                List<String[]> l = new ArrayList<>();
                for (int j = 0; j < it.length(); j++) {
                    JSONObject x = it.getJSONObject(j);
                    l.add(new String[]{x.getString("t"), x.getString("u")});
                }
                data.put(p.getString("name"), l);
            }
        } catch (Exception ignored) {
        }
    }

    private static String buildJson() {
        try {
            JSONArray arr = new JSONArray();
            for (String name : data.keySet()) {
                JSONArray items = new JSONArray();
                for (String[] it : data.get(name)) {
                    items.put(new JSONObject().put("t", it[0]).put("u", it[1]));
                }
                arr.put(new JSONObject().put("name", name).put("items", items));
            }
            return new JSONObject().put("playlists", arr).toString();
        } catch (Exception e) {
            return "{\"playlists\":[]}";
        }
    }

    private static void save() {
        prefs.edit().putString("data", buildJson()).apply();
        version++;
    }

    private static String cleanName(String n) {
        n = n == null ? "" : n.trim();
        return n.isEmpty() ? "My Playlist" : n;
    }

    public static synchronized String toJson() {
        return buildJson();
    }

    public static synchronized List<String> names() {
        return new ArrayList<>(data.keySet());
    }

    public static synchronized List<String[]> items(String name) {
        List<String[]> l = data.get(name);
        return l == null ? new ArrayList<String[]>() : new ArrayList<>(l);
    }

    public static synchronized void add(String name, String title, String url) {
        name = cleanName(name);
        List<String[]> l = data.get(name);
        if (l == null) {
            l = new ArrayList<>();
            data.put(name, l);
        }
        l.add(new String[]{title, url});
        save();
    }

    public static synchronized int addAll(String name, List<String[]> items) {
        name = cleanName(name);
        List<String[]> l = data.get(name);
        if (l == null) {
            l = new ArrayList<>();
            data.put(name, l);
        }
        l.addAll(items);
        save();
        return items.size();
    }

    public static synchronized void delItem(String name, int idx) {
        List<String[]> l = data.get(name);
        if (l != null && idx >= 0 && idx < l.size()) {
            l.remove(idx);
            save();
        }
    }

    public static synchronized void delPlaylist(String name) {
        if (data.remove(name) != null) save();
    }

    public static long pos(String url) {
        return posPrefs == null ? 0 : posPrefs.getLong(url, 0);
    }

    public static void setPos(String url, long p) {
        if (posPrefs != null) posPrefs.edit().putLong(url, p).apply();
    }

    /** Reads an M3U playlist, or a plain list of links, one per line. */
    public static List<String[]> parseM3u(String text) {
        List<String[]> out = new ArrayList<>();
        String title = null;
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTINF")) {
                int c = line.lastIndexOf(',');
                title = c >= 0 ? line.substring(c + 1).trim() : null;
            } else if (line.startsWith("#")) {
                continue;
            } else if (line.contains("://")) {
                out.add(new String[]{(title != null && !title.isEmpty()) ? title : line, line});
                title = null;
            }
        }
        return out;
    }
}
