package com.mytv.player;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/** Tiny web page so you can add links from your phone. */
public class WebServer extends NanoHTTPD {
    private final Context ctx;

    public WebServer(Context c, int port) {
        super(port);
        ctx = c.getApplicationContext();
    }

    @Override
    public Response serve(IHTTPSession s) {
        try {
            Map<String, String> files = new HashMap<>();
            if (Method.POST.equals(s.getMethod())) s.parseBody(files);
            Map<String, String> p = s.getParms();
            String uri = s.getUri();

            if ("/data".equals(uri)) return json(Store.toJson());

            if ("/add".equals(uri)) {
                String u = val(p, "url");
                if (!u.contains("://")) return fail("The link must start with http:// or https://");
                String t = val(p, "title");
                Store.add(val(p, "playlist"), t.isEmpty() ? u : t, u);
                return json("{\"ok\":true}");
            }

            if ("/import".equals(uri)) {
                String text = val(p, "text");
                String link = val(p, "link");
                if (text.isEmpty() && !link.isEmpty()) text = fetch(link);
                List<String[]> l = Store.parseM3u(text);
                if (l.isEmpty()) return fail("No video links found in that playlist.");
                int n = Store.addAll(val(p, "playlist"), l);
                return json("{\"ok\":true,\"added\":" + n + "}");
            }

            if ("/del".equals(uri)) {
                Store.delItem(val(p, "playlist"), Integer.parseInt(val(p, "idx")));
                return json("{\"ok\":true}");
            }

            if ("/delpl".equals(uri)) {
                Store.delPlaylist(val(p, "playlist"));
                return json("{\"ok\":true}");
            }

            if ("/".equals(uri) || "/index.html".equals(uri)) {
                try (InputStream in = ctx.getAssets().open("index.html");
                     ByteArrayOutputStream o = new ByteArrayOutputStream()) {
                    byte[] b = new byte[8192];
                    int n;
                    while ((n = in.read(b)) > 0) o.write(b, 0, n);
                    return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8",
                            o.toString("UTF-8"));
                }
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
        } catch (Exception e) {
            return fail(String.valueOf(e));
        }
    }

    private static String val(Map<String, String> p, String k) {
        String v = p.get(k);
        return v == null ? "" : v.trim();
    }

    private Response json(String s) {
        return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", s);
    }

    private Response fail(String msg) {
        try {
            return json(new JSONObject().put("ok", false).put("msg", msg).toString());
        } catch (Exception e) {
            return json("{\"ok\":false}");
        }
    }

    private static String fetch(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            int n, total = 0;
            while ((n = in.read(b)) > 0 && total < 5000000) {
                o.write(b, 0, n);
                total += n;
            }
            return o.toString("UTF-8");
        }
    }
}
