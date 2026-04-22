package com.mycompany.myapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.button1).setOnClickListener(v ->
            Toast.makeText(this, "チン！", Toast.LENGTH_SHORT).show());
        findViewById(R.id.button2).setOnClickListener(v ->
            Toast.makeText(this, "ブン！", Toast.LENGTH_SHORT).show());
        findViewById(R.id.button3).setOnClickListener(v ->
            Toast.makeText(this, "トゥース！", Toast.LENGTH_SHORT).show());
    }
}
