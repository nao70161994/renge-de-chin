package com.mycompany.myapp;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] soundIds = new int[3];
    private final boolean[] ready = new boolean[3];
    private final boolean[] failed = new boolean[3];
    private final int[] buttonIds = {R.id.button1, R.id.button2, R.id.button3};
    private SoundPool soundPool;
    private TextView display;
    private TextView status;
    private boolean destroyed;
    private final Runnable loadTimeout = () -> {
        for (int i = 0; i < ready.length; i++) if (!ready[i]) failed[i] = true;
        updateStatus();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        display = findViewById(R.id.display);
        status = findViewById(R.id.status);
        for (int id : buttonIds) findViewById(id).setEnabled(false);
        findViewById(R.id.button1).setOnClickListener(v -> play(0, v, R.string.bell_answer));
        findViewById(R.id.button2).setOnClickListener(v -> play(1, v, R.string.oven_answer));
        findViewById(R.id.button3).setOnClickListener(v -> play(2, v, R.string.toast_answer));
        findViewById(R.id.retry).setOnClickListener(v -> recreate());
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        soundPool = new SoundPool.Builder().setMaxStreams(3)
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        soundPool.setOnLoadCompleteListener((pool, id, result) -> {
            if (destroyed) return;
            for (int i = 0; i < soundIds.length; i++) {
                if (soundIds[i] != id) continue;
                ready[i] = result == 0;
                failed[i] = result != 0;
                findViewById(buttonIds[i]).setEnabled(ready[i]);
                if (result != 0) Log.e(TAG, "Sound load failed: " + id + ", status=" + result);
            }
            updateStatus();
        });
        String[] assets = {"bell.wav", "oven.wav", "toos.ogg"};
        for (int i = 0; i < assets.length; i++) {
            try (AssetFileDescriptor asset = getAssets().openFd(assets[i])) {
                soundIds[i] = soundPool.load(asset, 1);
                failed[i] = soundIds[i] == 0;
            } catch (IOException | RuntimeException e) {
                failed[i] = true;
                Log.e(TAG, "Unable to load " + assets[i], e);
            }
        }
        handler.postDelayed(loadTimeout, 10000);
        updateStatus();
    }

    private void updateStatus() {
        boolean allReady = true;
        boolean anyFailure = false;
        for (int i = 0; i < ready.length; i++) {
            allReady &= ready[i];
            anyFailure |= failed[i];
        }
        status.setText(anyFailure ? R.string.load_failed : allReady ? R.string.ready : R.string.loading);
        findViewById(R.id.retry).setVisibility(anyFailure ? View.VISIBLE : View.GONE);
        if (allReady) handler.removeCallbacks(loadTimeout);
    }

    private void play(int index, View button, int message) {
        if (!ready[index]) return;
        if (soundPool.play(soundIds[index], 1f, 1f, 0, 0, 1f) == 0) {
            Log.w(TAG, "Unable to start sound: " + soundIds[index]);
            status.setText(R.string.play_failed);
            return;
        }
        display.setText(message);
        display.announceForAccessibility(getString(message));
        display.animate().cancel();
        display.setScaleX(0.85f);
        display.setScaleY(0.85f);
        display.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
        button.animate().cancel();
        button.setScaleX(0.97f);
        button.setScaleY(0.97f);
        button.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
        updateStatus();
    }

    @Override
    protected void onStop() {
        soundPool.autoPause();
        display.animate().cancel();
        for (int id : buttonIds) findViewById(id).animate().cancel();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        soundPool.setOnLoadCompleteListener(null);
        soundPool.release();
        super.onDestroy();
    }
}
