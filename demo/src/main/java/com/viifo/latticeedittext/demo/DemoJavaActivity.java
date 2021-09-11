package com.viifo.latticeedittext.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import com.viifo.latticeedittext.LatticeEditText;
import com.viifo.latticeedittext.OnTextChangeListener;

import org.jetbrains.annotations.Nullable;

public class DemoJavaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        LatticeEditText latticeEditText = findViewById(R.id.et_input);
        latticeEditText.setOnTextChangeListener(new OnTextChangeListener() {
            @Override
            public void onTextChange(@Nullable String text) {
                System.out.println("--> OnTextChangeListener： text = " + text);
            }
        });
        findViewById(R.id.btn_get).setOnClickListener(v -> {
            ((TextView)findViewById(R.id.tv_text)).setText(latticeEditText.getText());
        });
    }
}