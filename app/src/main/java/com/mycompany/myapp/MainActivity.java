package com.mycompany.myapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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
    private int toosSoundId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        soundPool = new SoundPool.Builder().setMaxStreams(3).build();
        try {
            toosSoundId = soundPool.load(getAssets().openFd("toos.ogg"), 1);
        } catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundPool.release();
    }

    public void range(View v) {
        playSynth(0);
        showPopup(v, "チン！");
    }

    public void oven(View v) {
        playSynth(1);
        showPopup(v, "ブン！");
    }

    public void toast(View v) {
        if (toosSoundId != -1) soundPool.play(toosSoundId, 1f, 1f, 0, 0, 1f);
        showPopup(v, "トゥース！");
    }

    private void playSynth(final int type) {
        new Thread(() -> {
            short[] samples = type == 0 ? generateBell() : generateLightsaber();
            AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                samples.length * 2, AudioTrack.MODE_STATIC);
            track.write(samples, 0, samples.length);
            track.play();
            try { Thread.sleep(samples.length * 1000L / SAMPLE_RATE + 200); } catch (Exception e) {}
            track.release();
        }).start();
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

        handler.postDelayed(popup::dismiss, 1500);
    }
}
