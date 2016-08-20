package com.ardic.android.iotignitedemoapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG = "IoTIgniteDemoApp";

    private Button startButton;
    private Intent iotIgniteDemoAppService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUIComponents();
    }

    private void initUIComponents() {
        startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(this);

        }

    @Override
    public void onClick(View view) {
        if (view.equals(startButton)) {
            Intent intent = new Intent(MainActivity.this, SensorsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public  void onDestroy() {
        super.onDestroy();
    }
}
