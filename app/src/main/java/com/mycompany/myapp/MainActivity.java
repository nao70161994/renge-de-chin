package com.mycompany.myapp;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void range(View v) { showPopup(v, "チン！"); }
    public void oven(View v)  { showPopup(v, "ブン！"); }
    public void toast(View v) { showPopup(v, "トゥース！"); }

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

        anchor.postDelayed(popup::dismiss, 1500);
    }
}
