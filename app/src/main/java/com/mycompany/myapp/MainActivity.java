package com.mycompany.myapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int SAMPLE_RATE = 44100;
    private SoundPool soundPool;
    private static final String TAG = "MainActivity";
    private final Set<Integer> loadedSounds = new HashSet<>();
    private int toosSoundId, bellSoundId, ovenSoundId;
    private PopupWindow activePopup;
    private final Runnable dismissPopup = () -> {
        if (activePopup != null) {
            activePopup.dismiss();
            activePopup = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.button1).setOnClickListener(this::range);
        findViewById(R.id.button2).setOnClickListener(this::oven);
        findViewById(R.id.button3).setOnClickListener(this::toast);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);

        soundPool = new SoundPool.Builder().setMaxStreams(3)
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build();
        soundPool.setOnLoadCompleteListener((pool, id, status) -> {
            if (status == 0) loadedSounds.add(id);
            else Log.e(TAG, "Sound load failed: " + id + ", status=" + status);
        });
        bellSoundId = loadSynth("bell.wav", generateBell());
        ovenSoundId = loadSynth("oven.wav", generateLightsaber());
        try (AssetFileDescriptor asset = getAssets().openFd("toos.ogg")) {
            toosSoundId = soundPool.load(asset, 1);
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Unable to load toos.ogg", e);
        }
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(dismissPopup);
        dismissPopup.run();
        soundPool.autoPause();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        dismissPopup.run();
        soundPool.setOnLoadCompleteListener(null);
        soundPool.release();
        loadedSounds.clear();
        super.onDestroy();
    }

    private int loadSynth(String name, short[] samples) {
        File file = new File(getCacheDir(), name);
        int size = samples.length * 2;
        ByteBuffer wav = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN);
        wav.putInt(0x46464952).putInt(36 + size).putInt(0x45564157);
        wav.putInt(0x20746d66).putInt(16).putShort((short) 1).putShort((short) 1);
        wav.putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort((short) 2).putShort((short) 16);
        wav.putInt(0x61746164).putInt(size);
        for (short sample : samples) wav.putShort(sample);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(wav.array());
        } catch (IOException e) {
            Log.e(TAG, "Unable to write " + name, e);
            return 0;
        }
        return soundPool.load(file.getAbsolutePath(), 1);
    }

    private void playSound(int id) {
        if (loadedSounds.contains(id) && soundPool.play(id, 1f, 1f, 0, 0, 1f) == 0) {
            Log.w(TAG, "Sound could not start: " + id);
        }
    }

    public void range(View v) {
        playSound(bellSoundId);
        showPopup(v, "チン！");
    }

    public void oven(View v) {
        playSound(ovenSoundId);
        showPopup(v, "ブン！");
    }

    public void toast(View v) {
        playSound(toosSoundId);
        showPopup(v, "トゥース！");
    }

    private short[] generateBell() {
        int n = SAMPLE_RATE * 1200 / 1000;
        short[] s = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double decay = Math.exp(-t * 4.0);
            double wave = Math.sin(2 * Math.PI * 880 * t)
                        + 0.3 * Math.sin(2 * Math.PI * 880 * 2.76 * t)
                        + 0.1 * Math.sin(2 * Math.PI * 880 * 5.4 * t);
            s[i] = (short) (wave * decay * 10000);
        }
        return s;
    }

    private short[] generateLightsaber() {
        int n = SAMPLE_RATE * 250 / 1000;
        short[] s = new short[n];
        double p1 = 0, p2 = 0, p3 = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 500 * Math.exp(-t * 12) + 80;
            double dp = 2 * Math.PI * freq / SAMPLE_RATE;
            p1 += dp; p2 += dp * 1.5; p3 += dp * 2.0;
            double wave = Math.sin(p1) + 0.5 * Math.sin(p2) + 0.25 * Math.sin(p3);
            double env = Math.min(t * 60, 1.0) * Math.exp(-t * 8);
            s[i] = (short) (wave * env * 10000);
        }
        return s;
    }

    private void showPopup(View anchor, String message) {
        handler.removeCallbacks(dismissPopup);
        dismissPopup.run();
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        tv.setPadding(48, 24, 48, 24);
        tv.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC333333);
        bg.setCornerRadius(32);
        tv.setBackground(bg);

        PopupWindow popup = new PopupWindow(tv,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setElevation(8);
        popup.showAsDropDown(anchor, 0, -anchor.getHeight() - 120, Gravity.CENTER);

        activePopup = popup;
        handler.postDelayed(dismissPopup, 1500);
    }
}
