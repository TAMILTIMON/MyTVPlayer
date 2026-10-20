package com.mytv.player;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {
    private static final int PORT = 8080;

    private TextView header;
    private final List<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String current = null;      // playlist being viewed, or null for the playlist list
    private boolean hintOnly = false;
    private WebServer server;
    private int seen = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (Store.version != seen) refresh();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Store.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1E2327"));
        root.setPadding(dp(48), dp(32), dp(48), dp(24));

        header = new TextView(this);
        header.setTextSize(30);
        header.setTextColor(Color.WHITE);

        TextView info = new TextView(this);
        info.setTextSize(16);
        info.setTextColor(Color.parseColor("#9AA0A6"));
        info.setPadding(0, dp(6), 0, dp(16));

        try {
            server = new WebServer(this, PORT);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            info.setText("Add videos from your phone (same Wi-Fi): open  http://" + ip() + ":" + PORT);
        } catch (Exception e) {
            server = null;
            info.setText("Could not start the phone page. Is another app using port " + PORT + "?");
        }

        ListView listView = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getView(position, convertView, parent);
                t.setTextSize(22);
                t.setTextColor(Color.WHITE);
                t.setPadding(dp(16), dp(14), dp(16), dp(14));
                return t;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (hintOnly) return;
            if (current == null) {
                current = Store.names().get(position);
                refresh();
            } else {
                Intent i = new Intent(this, PlayerActivity.class);
                i.putExtra("playlist", current);
                i.putExtra("index", position);
                startActivity(i);
            }
        });

        root.addView(header);
        root.addView(info);
        root.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        listView.requestFocus();
        refresh();
    }

    private void refresh() {
        seen = Store.version;
        rows.clear();
        if (current == null) {
            header.setText("My TV Player");
            List<String> names = Store.names();
            for (String n : names) rows.add(n + "   (" + Store.items(n).size() + ")");
            hintOnly = names.isEmpty();
            if (hintOnly) rows.add("No playlists yet. Open the address above on your phone to add videos.");
        } else {
            if (!Store.names().contains(current)) {
                current = null;
                refresh();
                return;
            }
            header.setText(current);
            List<String[]> items = Store.items(current);
            for (String[] it : items) rows.add(it[0]);
            hintOnly = items.isEmpty();
            if (hintOnly) rows.add("This playlist is empty.");
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }

    @Override
    public void onBackPressed() {
        if (current != null) {
            current = null;
            refresh();
        } else {
            super.onBackPressed();
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String ip() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!a.isLoopbackAddress() && a instanceof Inet4Address) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        return "TV-IP";
    }
}
