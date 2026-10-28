package com.amolsolunke.stepfitpro;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private TextView stepsText, distanceText, caloriesText;
    private TextView goalText, statusText, remainingText;
    private ProgressBar progressBar;
    private EditText goalInput;
    private SharedPreferences prefs;

    private int dailyGoal = 10000;
    private int steps = 0;
    private int baseline = -1;
    private String today;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("stepfit", MODE_PRIVATE);
        dailyGoal = prefs.getInt("goal", 10000);
        today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());

        String savedDate = prefs.getString("date", "");

        if (today.equals(savedDate)) {
            steps = prefs.getInt("steps", 0);
            baseline = prefs.getInt("baseline", -1);
        } else {
            steps = 0;
            baseline = -1;
            prefs.edit()
                    .putString("date", today)
                    .putInt("steps", 0)
                    .putInt("baseline", -1)
                    .apply();
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(28, 20, 28, 20);
        layout.setBackgroundColor(Color.rgb(12, 20, 40));

        TextView title = makeText("👟 StepFit PRO", 30, Color.CYAN);
        layout.addView(title);

        stepsText = makeText("", 34, Color.WHITE);
        layout.addView(stepsText);

        distanceText = makeText("", 22, Color.WHITE);
        layout.addView(distanceText);

        caloriesText = makeText("", 22, Color.WHITE);
        layout.addView(caloriesText);

        goalText = makeText("", 21, Color.GREEN);
        layout.addView(goalText);

        progressBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(dailyGoal);

        layout.addView(progressBar,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 30));

        remainingText = makeText("", 18, Color.WHITE);
        layout.addView(remainingText);

        TextView chooseGoal = makeText("तुमचे दैनिक लक्ष्य ठरवा", 20, Color.CYAN);
        layout.addView(chooseGoal);

        goalInput = new EditText(this);
        goalInput.setSingleLine(true);
        goalInput.setHint("उदा. 8000");
        goalInput.setInputType(2);
        goalInput.setTextColor(Color.WHITE);
        goalInput.setHintTextColor(Color.LTGRAY);
        goalInput.setText(String.valueOf(dailyGoal));
        goalInput.setGravity(Gravity.CENTER);

        layout.addView(goalInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        Button setGoalButton = new Button(this);
        setGoalButton.setText("लक्ष्य सेट करा");
        layout.addView(setGoalButton);

        statusText = makeText("", 16, Color.LTGRAY);
        layout.addView(statusText);

        setContentView(layout);

        setGoalButton.setOnClickListener(v -> {
            String value = goalInput.getText().toString().trim();

            try {
                int newGoal = Integer.parseInt(value);

                if (newGoal < 1 || newGoal > 1000000) {
                    Toast.makeText(this,
                            "कृपया 1 ते 10,00,000 मधील लक्ष्य टाका",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                dailyGoal = newGoal;
                prefs.edit().putInt("goal", dailyGoal).apply();

                progressBar.setMax(dailyGoal);
                updateScreen();

                Toast.makeText(this,
                        "तुमचे लक्ष्य जतन झाले!",
                        Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(this,
                        "कृपया योग्य संख्या टाका",
                        Toast.LENGTH_SHORT).show();
            }
        });

        updateScreen();

        sensorManager = (SensorManager)
                getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(
                    Sensor.TYPE_STEP_COUNTER);
        }

        if (stepSensor == null) {
            statusText.setText(
                    "या फोनमध्ये स्टेप-काउंटर सेन्सर उपलब्ध नाही.");
        } else {
            statusText.setText(
                    "पावले मोजण्यासाठी फोन सोबत घेऊन चाला.");
            checkActivityPermission();
        }
    }

    private TextView makeText(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, 10, 0, 10);
        return view;
    }

    private void checkActivityPermission() {
        if (Build.VERSION.SDK_INT >= 29 &&
                checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    100);
        }
    }

    private void updateScreen() {
        if (stepsText == null) return;

        double distance = steps * 0.00075;
        double calories = steps * 0.04;
        int remaining = Math.max(0, dailyGoal - steps);
        int percent = (int) Math.min(100,
                (steps * 100.0) / dailyGoal);

        stepsText.setText("आजची पावले\n" + steps);

        distanceText.setText(String.format(Locale.getDefault(),
                "🛣️ अंतर : %.2f किमी", distance));

        caloriesText.setText(String.format(Locale.getDefault(),
                "🔥 कॅलरी : %.1f kcal", calories));

        goalText.setText("🎯 दैनिक लक्ष्य : " + dailyGoal
                + "\nपूर्ण : " + percent + "%");

        progressBar.setMax(dailyGoal);
        progressBar.setProgress(Math.min(steps, dailyGoal));

        if (remaining == 0) {
            remainingText.setText("🎉 अभिनंदन! लक्ष्य पूर्ण झाले.");
        } else {
            remainingText.setText("आणखी " + remaining
                    + " पावले चालायची आहेत.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null && stepSensor != null) {
            if (Build.VERSION.SDK_INT < 29 ||
                    checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED) {
                sensorManager.registerListener(
                        this, stepSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
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
            int total = (int) event.values[0];

            if (baseline < 0 || total < baseline) {
                baseline = total;
            }

            steps = Math.max(0, total - baseline);

            prefs.edit()
                    .putString("date", today)
                    .putInt("baseline", baseline)
                    .putInt("steps", steps)
                    .apply();

            updateScreen();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusText.setText(
                        "परवानगी मिळाली. आता चालायला सुरुवात करा!");
            } else {
                statusText.setText(
                        "पावले मोजण्यासाठी Activity Recognition परवानगी द्या.");
            }
        }
    }
            }
