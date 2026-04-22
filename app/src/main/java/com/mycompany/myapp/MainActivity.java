package com.mycompany.myapp;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.button1).setOnClickListener(v -> showPopup(v, "チン！"));
        findViewById(R.id.button2).setOnClickListener(v -> showPopup(v, "ブン！"));
        findViewById(R.id.button3).setOnClickListener(v -> showPopup(v, "トゥース！"));
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
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setElevation(8);
        popup.showAsDropDown(anchor, 0, -anchor.getHeight() - 120, Gravity.CENTER);

        anchor.postDelayed(popup::dismiss, 1500);
    }
}
