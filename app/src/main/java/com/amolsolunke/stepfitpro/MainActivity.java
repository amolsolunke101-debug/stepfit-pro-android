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
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private TextView stepsText, goalText, distanceText, caloriesText;
    private EditText goalInput;
    private Button goalButton;
    private SharedPreferences preferences;

    private int steps = 0;
    private int dailyGoal = 6000;
    private float strideMeters = 0.75f;

    private final int backgroundColor = Color.rgb(16, 25, 50);
    private final int accentColor = Color.rgb(0, 220, 180);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences("StepFitData", MODE_PRIVATE);
        dailyGoal = preferences.getInt("daily_goal", 6000);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(28, 36, 28, 28);
        layout.setBackgroundColor(backgroundColor);

        TextView title = new TextView(this);
        title.setText("StepFit PRO");
        title.setTextSize(32);
        title.setTypeface(null, 1);
        title.setTextColor(accentColor);
        title.setGravity(Gravity.CENTER);
        layout.addView(title, matchWidth());

        TextView subtitle = new TextView(this);
        subtitle.setText("YOUR DAILY WALKING PARTNER");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 32);
        layout.addView(subtitle, matchWidth());

        stepsText = makeText("आजची पावले\n0", 30, Color.WHITE);
        layout.addView(stepsText, matchWidth());

        goalText = makeText("", 18, accentColor);
        goalText.setPadding(0, 18, 0, 10);
        layout.addView(goalText, matchWidth());

        distanceText = makeText("अंतर: 0.00 किमी", 20, Color.WHITE);
        distanceText.setPadding(0, 12, 0, 12);
        layout.addView(distanceText, matchWidth());

        caloriesText = makeText("अंदाजे कॅलरी: 0", 20, Color.WHITE);
        caloriesText.setPadding(0, 12, 0, 24);
        layout.addView(caloriesText, matchWidth());

        TextView goalLabel = makeText("तुमचे दैनिक पावलांचे लक्ष्य", 17, Color.WHITE);
        layout.addView(goalLabel, matchWidth());

        goalInput = new EditText(this);
        goalInput.setSingleLine(true);
        goalInput.setTextColor(Color.WHITE);
        goalInput.setHintTextColor(Color.LTGRAY);
        goalInput.setHint("उदा. 6000");
        goalInput.setInputType(2);
        goalInput.setText(String.valueOf(dailyGoal));
        goalInput.setGravity(Gravity.CENTER);
        layout.addView(goalInput, matchWidth());

        goalButton = new Button(this);
        goalButton.setText("लक्ष्य जतन करा");
        layout.addView(goalButton, matchWidth());

        goalButton.setOnClickListener(v -> saveGoal());

        TextView note = makeText(
                "चालायला सुरुवात करा आणि तुमचे लक्ष्य पूर्ण करा!",
                15, Color.LTGRAY);
        note.setPadding(0, 28, 0, 0);
        layout.addView(note, matchWidth());

        setContentView(layout);
        updateDisplay();
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private TextView makeText(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private void saveGoal() {
        String value = goalInput.getText().toString().trim();

        if (value.isEmpty()) {
            Toast.makeText(this, "कृपया लक्ष्य लिहा", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int newGoal = Integer.parseInt(value);

            if (newGoal < 1 || newGoal > 1000000) {
                Toast.makeText(this,
                        "लक्ष्य 1 ते 1000000 दरम्यान ठेवा",
                        Toast.LENGTH_LONG).show();
                return;
            }

            dailyGoal = newGoal;
            preferences.edit().putInt("daily_goal", dailyGoal).apply();

            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(goalInput.getWindowToken(), 0);
            }

            goalInput.clearFocus();
            updateDisplay();

            Toast.makeText(this, "दैनिक लक्ष्य जतन झाले!",
                    Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "योग्य संख्या लिहा",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDisplay() {
        if (stepsText == null) return;

        stepsText.setText("आजची पावले\n" + steps);
        goalText.setText("लक्ष्य: " + dailyGoal + " पावले");

        float distanceKm = (steps * strideMeters) / 1000f;
        int calories = Math.round(steps * 0.04f);

        distanceText.setText(String.format(
                java.util.Locale.US, "अंतर: %.2f किमी", distanceKm));

        caloriesText.setText("अंदाजे कॅलरी: " + calories);

        if (steps >= dailyGoal) {
            goalText.setText("🎉 लक्ष्य पूर्ण! " + steps + " / " + dailyGoal);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null && stepSensor != null) {
            sensorManager.registerListener(
                    this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this,
                    "या फोनमध्ये पावले मोजणारा सेन्सर उपलब्ध नाही.",
                    Toast.LENGTH_LONG).show();
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
            steps = Math.max(0, (int) event.values[0]);
            updateDisplay();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accuracy update is not required.
    }
  }
