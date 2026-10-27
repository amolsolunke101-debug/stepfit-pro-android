package com.amolsolunke.stepfitpro;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

private SensorManager sensorManager;
private Sensor stepSensor;
private TextView stepsText;
private int steps = 0;

@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setGravity(Gravity.CENTER);
    layout.setPadding(30, 30, 30, 30);
    layout.setBackgroundColor(Color.rgb(10, 25, 50));

    TextView title = new TextView(this);
    title.setText("StepFit PRO");
    title.setTextSize(30);
    title.setTextColor(Color.CYAN);
    title.setGravity(Gravity.CENTER);
    layout.addView(title);

    stepsText = new TextView(this);
    stepsText.setText("आजची पावले\n0");
    stepsText.setTextSize(28);
    stepsText.setTextColor(Color.WHITE);
    stepsText.setGravity(Gravity.CENTER);
    stepsText.setPadding(20, 60, 20, 60);
    layout.addView(stepsText);

    TextView info = new TextView(this);
    info.setText("चालायला सुरुवात करा!");
    info.setTextSize(18);
    info.setTextColor(Color.WHITE);
    info.setGravity(Gravity.CENTER);
    layout.addView(info);

    setContentView(layout);
}

@Override
protected void onResume() {
    super.onResume();
    if (sensorManager != null && stepSensor != null) {
        sensorManager.registerListener(this, stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }
}

@Override
protected void onPause() {
    super.onPause();
    if (sensorManager != null) {
        sensorManager.unregisterListener(this);
    }
}

@Override
public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
        steps = (int) event.values[0];
        stepsText.setText("पावले\n" + steps);
    }
}

@Override
public void onAccuracyChanged(Sensor sensor, int accuracy) {
}

                                                }
