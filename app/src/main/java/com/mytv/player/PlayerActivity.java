package com.mytv.player;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView view;
    private List<String[]> items;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saver = new Runnable() {
        @Override
        public void run() {
            savePosition();
            handler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String name = getIntent().getStringExtra("playlist");
        int start = getIntent().getIntExtra("index", 0);
        items = Store.items(name);
        if (items.isEmpty()) {
            finish();
            return;
        }
        if (start < 0 || start >= items.size()) start = 0;

        view = new PlayerView(this);
        view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        view.setControllerShowTimeoutMs(4000);
        setContentView(view);

        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 9; MyTVPlayer) AppleWebKit/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000);
        DefaultDataSource.Factory dataSource = new DefaultDataSource.Factory(this, http);
        // Small buffers: this TV only has 1 GB of RAM.
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(15000, 30000, 1500, 3000)
                .build();
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(this)
                .setEnableDecoderFallback(true);

        player = new ExoPlayer.Builder(this, renderers)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSource))
                .setLoadControl(loadControl)
                .build();
        view.setPlayer(player);

        List<MediaItem> list = new ArrayList<>();
        for (String[] it : items) {
            list.add(new MediaItem.Builder()
                    .setUri(it[1])
                    .setMediaMetadata(new MediaMetadata.Builder().setTitle(it[0]).build())
                    .build());
        }
        long resume = Store.pos(items.get(start)[1]);
        player.setMediaItems(list, start, resume > 5000 ? resume : 0);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(PlayerActivity.this,
                        "Cannot play this link: " + error.getErrorCodeName(), Toast.LENGTH_LONG).show();
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                    player.prepare();
                }
            }

            @Override
            public void onMediaItemTransition(MediaItem item, int reason) {
                int idx = player.getCurrentMediaItemIndex();
                if (idx >= 0 && idx < items.size()) {
                    Toast.makeText(PlayerActivity.this, items.get(idx)[0], Toast.LENGTH_SHORT).show();
                }
            }
        });

        player.prepare();
        player.setPlayWhenReady(true);
    }

    private boolean handleKey(int code) {
        if (player == null) return false;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                player.setPlayWhenReady(!player.getPlayWhenReady());
                view.showController();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
                view.showController();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                player.seekTo(player.getCurrentPosition() + 10000);
                view.showController();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                player.seekToNextMediaItem();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                player.seekToPreviousMediaItem();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && handleKey(event.getKeyCode())) return true;
        return super.dispatchKeyEvent(event);
    }

    private void savePosition() {
        if (player == null || items == null) return;
        int idx = player.getCurrentMediaItemIndex();
        if (idx < 0 || idx >= items.size()) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        boolean nearEnd = dur > 0 && pos > dur - 10000;
        Store.setPos(items.get(idx)[1], nearEnd ? 0 : pos);
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.postDelayed(saver, 15000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(saver);
        savePosition();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(saver);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
